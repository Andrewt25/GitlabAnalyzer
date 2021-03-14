package com.eris.gitlabanalyzer.controller;

import com.eris.gitlabanalyzer.model.gitlabresponse.*;
import com.eris.gitlabanalyzer.service.AuthService;
import com.eris.gitlabanalyzer.service.GitLabService;
import com.eris.gitlabanalyzer.service.ProjectService;
import com.eris.gitlabanalyzer.service.UserServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.OffsetDateTime;

@RestController
@RequestMapping(path = "/api/v1/gitlab")
public class GitLabController {

    private final AuthService authService;
    private final UserServerService userServerService;
    private final ProjectService projectService;

    @Autowired
    public GitLabController(AuthService authService, UserServerService userServerService, ProjectService projectService) {
        this.authService = authService;
        this.userServerService = userServerService;
        this.projectService = projectService;
    }

    public GitLabService initGitLabService(Principal principal, Long projectId) {
        var user = authService.getLoggedInUser(principal);
        var project = projectService.getProject(projectId).orElseThrow(()
                -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find Project."));
        var server = project.getServer();
        var userServer = userServerService.getUserServer(user, server.getId()).orElseThrow(()
                -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find UserServer."));
        var gitLabService = new GitLabService(userServer.getServer().getServerUrl(), userServer.getAccessToken());
        return gitLabService;
    }

    @GetMapping(path ="{serverId}/projects")
    public Flux<GitLabProject> getProjects(Principal principal, @PathVariable("serverId") Long id) {
        var user = authService.getLoggedInUser(principal);
        var userServer = userServerService.getUserServer(user, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find server."));
        var gitLabService = new GitLabService(userServer.getServer().getServerUrl(), userServer.getAccessToken());
        return gitLabService.getProjects();
    }

    // Used in notes page for now
    @GetMapping(path ="/projects/{projectId}")
    public Mono<GitLabProject> getProject(Principal principal, @PathVariable("projectId") Long projectId) {
        var gitLabService = initGitLabService(principal, projectId);
        return gitLabService.getProject(projectId);
    }

    // TODO: currently there is no direct use for this endpoint, to be removed
    @GetMapping(path ="/projects/{projectId}/merge_requests")
    public Flux<GitLabMergeRequest> getMergeRequests(
            Principal principal,
            @PathVariable("projectId") Long projectId,
            @RequestParam("startDateTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDateTime,
            @RequestParam("endDateTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDateTime) {

        var gitLabService = initGitLabService(principal, projectId);
        return gitLabService.getMergeRequests(projectId, startDateTime, endDateTime);
    }

    // TODO: currently there is no direct use for this endpoint, to be removed
    @GetMapping(path ="/projects/{projectId}/merge_request/{merge_request_iid}/commits")
    public Flux<GitLabCommit> getMergeRequestCommits(
            Principal principal,
            @PathVariable("projectId") Long projectId,
            @PathVariable("merge_request_iid") Long merge_request_iid)  {

        var gitLabService = initGitLabService(principal, projectId);
        return gitLabService.getMergeRequestCommits(projectId, merge_request_iid);
    }

    // TODO: currently there is no direct use for this endpoint, to be removed
    @GetMapping(path ="/projects/{projectId}/commits")
    public Flux<GitLabCommit> getCommits(
            Principal principal,
            @PathVariable("projectId") Long projectId,
            @RequestParam("startDateTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDateTime,
            @RequestParam("endDateTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDateTime) {

        var gitLabService = initGitLabService(principal, projectId);
        return gitLabService.getCommits(projectId, startDateTime, endDateTime);
    }

    // TODO: currently there is no direct use for this endpoint, to be removed
    @GetMapping(path ="/projects/{projectId}/commit/{sha}/diff")
    public Flux<GitLabFileChange> getCommitDiff(
            Principal principal,
            @PathVariable("projectId") Long projectId,
            @PathVariable("sha") String sha) {

        var gitLabService = initGitLabService(principal, projectId);
        return gitLabService.getCommitDiff(projectId, sha);
    }

    // TODO: currently there is no direct use for this endpoint, to be removed
    @GetMapping(path ="/projects/{projectId}/merge_request/{merge_request_iid}/diff")
    public Flux<GitLabFileChange> getMergeDiff(
            Principal principal,
            @PathVariable("projectId") Long projectId,
            @PathVariable("merge_request_iid") Long merge_request_iid) {

        var gitLabService = initGitLabService(principal, projectId);
        return gitLabService.getMergeRequestDiff(projectId, merge_request_iid);
    }

    // Used in notes page for now
    @GetMapping(path = "/projects/{projectId}/merge_requests/{merge_request_iid}/notes")
    public Flux<GitLabMergeRequestNote> getMergeRequestNotes(
            Principal principal,
            @PathVariable("projectId") Long projectId,
            @PathVariable("merge_request_iid") Long merge_request_iid) {

        var gitLabService = initGitLabService(principal, projectId);
        return gitLabService.getMergeRequestNotes(projectId, merge_request_iid);
    }

    // Used in notes page for now
    @GetMapping(path = "/projects/{projectId}/issues")
    public Flux<GitLabIssue> getIssues(
            Principal principal,
            @PathVariable("projectId") Long projectId,
            @RequestParam("startDateTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDateTime,
            @RequestParam("endDateTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDateTime) {

        var gitLabService = initGitLabService(principal, projectId);
        return gitLabService.getIssues(projectId, startDateTime, endDateTime);
    }

    // Used in notes page for now
    @GetMapping(path = "/projects/{projectId}/issues/{issue_iid}/notes")
    public Flux<GitLabIssueNote> getIssueNotes(
            Principal principal,
            @PathVariable("projectId") Long projectId,
            @PathVariable("issue_iid") Long issue_iid) {

        var gitLabService = initGitLabService(principal, projectId);
        return gitLabService.getIssueNotes(projectId, issue_iid);
    }
    @GetMapping(path ="/projects/{projectId}/members")
    public Flux<GitLabMember> getMembers(
            Principal principal,
            @PathVariable("projectId") Long projectId) {

        var gitLabService = initGitLabService(principal, projectId);
        return gitLabService.getMembers(projectId);
    }
}
