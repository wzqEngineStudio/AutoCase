package com.autocase.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * GitHub配置实体 - 存储GitHub仓库连接信息
 */
public class GithubConfig {

    private String repositoryUrl;
    private String username;
    private String token;

    public GithubConfig() {
    }

    public GithubConfig(String repositoryUrl, String username, String token) {
        this.repositoryUrl = repositoryUrl;
        this.username = username;
        this.token = token;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @JsonIgnore
    public boolean isConfigured() {
        return repositoryUrl != null && !repositoryUrl.isEmpty()
                && username != null && !username.isEmpty()
                && token != null && !token.isEmpty();
    }
}
