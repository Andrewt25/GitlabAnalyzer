package com.eris.gitlabanalyzer.service;

import com.eris.gitlabanalyzer.model.*;
import com.eris.gitlabanalyzer.model.gitlabresponse.GitLabCommit;
import com.eris.gitlabanalyzer.model.gitlabresponse.GitLabMergeRequest;
import com.eris.gitlabanalyzer.repository.ProjectRepository;
import com.eris.gitlabanalyzer.repository.ServerRepository;
import com.eris.gitlabanalyzer.repository.UserProjectPermissionRepository;
import com.eris.gitlabanalyzer.repository.UserServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ServerRepository serverRepository;
    private final UserServerRepository userServerRepository;
    private final UserProjectPermissionRepository userProjectPermissionRepository;
    private String serverUrl;
    private String accessToken;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, ServerRepository serverRepository,
                          UserServerRepository userServerRepository, UserProjectPermissionRepository userProjectPermissionRepository) {
        this.projectRepository = projectRepository;
        this.serverRepository = serverRepository;
        this.userServerRepository = userServerRepository;
        this.userProjectPermissionRepository = userProjectPermissionRepository;
    }

    public void setServerUrlAndAccessToken(String serverUrl, String accessToken) {
        this.serverUrl = serverUrl;
        this.accessToken = accessToken;
    }

    public Project saveProjectInfo(Long projectId) {
        // TODO use an internal projectId to find the correct server
        var gitLabService = new GitLabService(serverUrl, accessToken);


        var gitLabProject = gitLabService.getProject(projectId).block();
        Server server = serverRepository.findByServerUrlAndAccessToken(serverUrl, accessToken);

        Project project = new Project(
                projectId,
                gitLabProject.getName(),
                gitLabProject.getNameWithNamespace(),
                gitLabProject.getWebUrl(),
                server
        );

        Optional<UserServer> userServer = userServerRepository.findUserServerByAccessToken(accessToken);
        User user;

        if (userServer.isPresent()) {
            user = userServer.get().getUser();
            UserProjectPermission userProjectPermission = new UserProjectPermission(
                    user,
                    project,
                    server
            );

            user.addProjectPermission(userProjectPermission);

        } else {
            throw new AccessDeniedException("Corresponding UserServer row does not exist in DB.");
        }

        return projectRepository.save(project);
    }

    public RawTimeLineProjectData getTimeLineProjectData(Long gitLabProjectId, OffsetDateTime startDateTime, OffsetDateTime endDateTime) {
        // TODO use an internal projectId to find the correct server
        var gitLabService = new GitLabService(serverUrl, accessToken);
        var mergeRequests = gitLabService.getMergeRequests(gitLabProjectId, startDateTime, endDateTime);

        // for all items in mergeRequests call get commits
            // for all items in commits call get diff
            // for all items in merge request get diff
        var rawMergeRequestData = mergeRequests
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .map((mergeRequest) -> getRawMergeRequestData(gitLabService, mergeRequest, gitLabProjectId))
                .sorted((mr1, mr2) -> (int)(mr1.getGitLabMergeRequest().getIid() - mr2.getGitLabMergeRequest().getIid()));


        // for all commits NOT in merge commits get diff
        var mergeRequestCommitIds = getMergeRequestCommitIds(rawMergeRequestData);
        var commits = gitLabService.getCommits(gitLabProjectId, startDateTime, endDateTime);
        var orphanCommits = getOrphanCommits(commits, mergeRequestCommitIds);
        var rawOrphanCommitData = orphanCommits
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .map((commit) -> getRawCommitData(gitLabService, commit, gitLabProjectId))
                .sorted(Comparator.comparing(c -> c.getGitLabCommit().getCreatedAt()));

        var rawProjectData = new RawTimeLineProjectData(gitLabProjectId, startDateTime, endDateTime, rawMergeRequestData, rawOrphanCommitData);

        return rawProjectData;
    }


    private RawMergeRequestData getRawMergeRequestData(GitLabService gitLabService, GitLabMergeRequest mergeRequest, Long gitLabProjectId) {
        var gitLabCommits = gitLabService.getMergeRequestCommits(gitLabProjectId, mergeRequest.getIid());
        var rawCommitData = gitLabCommits
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .map((commit) -> getRawCommitData(gitLabService, commit, gitLabProjectId))
                .sorted(Comparator.comparing(c -> c.getGitLabCommit().getCreatedAt()));

        var gitLabDiff = gitLabService.getMergeRequestDiff(gitLabProjectId, mergeRequest.getIid());

        var rawMergeRequestData = new RawMergeRequestData(rawCommitData, gitLabDiff, mergeRequest);
        return rawMergeRequestData;
    }

    private RawCommitData getRawCommitData(GitLabService gitLabService, GitLabCommit commit, Long gitLabProjectId) {
        var changes = gitLabService.getCommitDiff(gitLabProjectId, commit.getSha());
        var rawCommitData = new RawCommitData(commit, changes);
        return rawCommitData;
    }

    private Mono<Set<String>> getMergeRequestCommitIds(Flux<RawMergeRequestData> mergeRequestData) {
        return mergeRequestData.flatMap(mergeRequest -> mergeRequest.getFluxRawCommitData())
                .map(commit -> commit.getFluxGitLabCommit().getSha())
                .collect(Collectors.toSet());
    }

    private Flux<GitLabCommit> getOrphanCommits(Flux<GitLabCommit> commits, Mono<Set<String>> mrCommitIds) {
        return mrCommitIds.flatMapMany(commitIds -> commits.filter(gitLabCommit -> !commitIds.contains(gitLabCommit.getSha())));
    }

    public List<Project> getProjects() {
        return projectRepository.findAll();
    }

    public Optional<Project> getProject(Long projectId) {
        return projectRepository.findById(projectId);
    }
}
