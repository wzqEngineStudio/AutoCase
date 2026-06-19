package com.autocase.logic;

import com.autocase.entity.GithubConfig;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * GitHub逻辑类 - 处理Git仓库初始化、提交和推送操作
 */
public class GithubLogic {

    private final GithubConfig config;

    public GithubLogic(GithubConfig config) {
        this.config = config;
    }

    /**
     * 初始化或打开Git仓库
     */
    private Git openOrCreateRepo(String directory) throws IOException, GitAPIException {
        File dir = new File(directory);
        File gitDir = new File(dir, ".git");

        if (gitDir.exists()) {
            return Git.open(dir);
        } else {
            Git git = Git.init().setDirectory(dir).call();

            // 配置用户信息
            git.getRepository().getConfig().setString("user", null, "name", config.getUsername());
            git.getRepository().getConfig().setString("user", null, "email", config.getUsername() + "@github.com");
            git.getRepository().getConfig().save();

            return git;
        }
    }

    /**
     * 设置远程仓库URL
     */
    private void setRemoteUrl(Git git) throws GitAPIException, URISyntaxException {
        // 移除已存在的origin
        try {
            git.remoteRemove().setRemoteName("origin").call();
        } catch (Exception e) {
            // ignore if no remote exists
        }

        // 添加新的origin
        git.remoteAdd()
                .setName("origin")
                .setUri(new URIish(config.getRepositoryUrl()))
                .call();
    }

    /**
     * 推送用例目录到GitHub
     * @param directory 用例目录路径
     * @param commitMessage 提交信息
     * @return 是否推送成功
     */
    public boolean pushToGithub(String directory, String commitMessage) {
        if (!config.isConfigured()) {
            System.err.println("GitHub配置未完成");
            return false;
        }

        Git git = null;
        try {
            git = openOrCreateRepo(directory);

            // 添加所有文件
            git.add().addFilepattern(".").call();

            // 提交
            PersonIdent person = new PersonIdent(config.getUsername(), config.getUsername() + "@github.com");
            git.commit()
                    .setMessage(commitMessage)
                    .setAuthor(person)
                    .call();

            // 设置远程仓库
            setRemoteUrl(git);

            // 推送
            CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(
                    config.getUsername(), config.getToken());

            git.push()
                    .setCredentialsProvider(credentialsProvider)
                    .setRemote("origin")
                    .call();

            return true;
        } catch (GitAPIException | IOException | URISyntaxException e) {
            System.err.println("GitHub推送失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (git != null) {
                git.close();
            }
        }
    }

    /**
     * 验证GitHub配置是否有效
     */
    public boolean validateConfig() {
        if (!config.isConfigured()) {
            return false;
        }

        // 验证URL格式
        String url = config.getRepositoryUrl();
        return url.startsWith("https://") || url.startsWith("git@");
    }
}
