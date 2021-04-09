package com.eris.gitlabanalyzer.service;

import com.eris.gitlabanalyzer.model.*;
import com.eris.gitlabanalyzer.repository.ServerRepository;
import com.eris.gitlabanalyzer.repository.UserServerRepository;
import com.eris.gitlabanalyzer.viewmodel.UserServerRequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class UserServerService {
    private final ServerRepository serverRepository;
    private final UserServerRepository userServerRepository;

    public UserServerService(ServerRepository serverRepository, UserServerRepository userServerRepository) {
        this.serverRepository = serverRepository;
        this.userServerRepository = userServerRepository;
    }

    public Optional<UserServer> getUserServer(User user, Long serverId) {
        var userServers = this.getUserServers(user);
        return userServers.stream().filter(s -> s.getServer().getId() == serverId).findFirst();
    }

    public List<UserServer> getUserServers(User user) {
        return userServerRepository.findUserServerByUserId(user.getId());
    }

    public UserServer createUserServer(User user, String serverUrl, String accessToken) {
        Optional<Server> serverByUser = serverRepository.findByServerUrlAndUserId(serverUrl, user.getId());
        if (serverByUser.isPresent()) {
            throw new IllegalStateException("Server already registered.");
        }

        UserServer userServer = new UserServer(accessToken);
        userServer.setUser(user);

        Optional<Server> ServerByUrl = serverRepository.findByServerUrl(serverUrl);
        if (ServerByUrl.isPresent()) {
            userServer.setServer(ServerByUrl.get());
        }
        else {
            var server = serverRepository.save(new Server(serverUrl));
            userServer.setServer(server);
        }
        return userServerRepository.save(userServer);
    }

    public UserServer updateUserServer(User user, Long serverId, String accessToken) {
        var userServer = userServerRepository.findUserServerByUserIdAndServerId(user.getId(), serverId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User server not found."));
        userServer.setAccessToken(accessToken);
        return userServerRepository.save(userServer);
    }

    public void deleteUserServer(User user, Long serverId) {
        var userServer = userServerRepository.findUserServerByUserIdAndServerId(user.getId(), serverId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User server not found."));
        userServerRepository.delete(userServer);
    }

}
