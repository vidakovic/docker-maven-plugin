package net.wouterdanes.docker.provider;

import com.google.common.base.Optional;
import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.*;
import net.wouterdanes.docker.provider.model.ContainerStartConfiguration;
import net.wouterdanes.docker.provider.model.ExposedPort;
import net.wouterdanes.docker.provider.model.ImageBuildConfiguration;
import net.wouterdanes.docker.remoteapi.model.ContainerInspectionResult;
import net.wouterdanes.docker.remoteapi.model.Credentials;
import net.wouterdanes.docker.remoteapi.util.DockerHostFromEnvironmentSupplier;
import net.wouterdanes.docker.remoteapi.util.DockerHostFromPropertySupplier;
import net.wouterdanes.docker.remoteapi.util.DockerPortFromEnvironmentSupplier;
import net.wouterdanes.docker.remoteapi.util.DockerPortFromPropertySupplier;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

import java.nio.file.Paths;
import java.util.*;

public class SpotifyDockerProvider implements DockerProvider {

    private Log log;

    private DockerClient docker;

    private Credentials credentials;

    private static final int DEFAULT_DOCKER_PORT = 2375;

    @Override
    public void setCredentials(Credentials credentials) {
        // TODO: find out how to set this in Spotify docker client
        this.credentials = credentials;

        try {
            // TODO: configuration connection pool and timeouts?
            DefaultDockerClient.Builder builder = DefaultDockerClient.fromEnv().readTimeoutMillis(60000);

            if(getDockerHostFromEnvironment().isPresent()) {
                // TODO: configure protocol (http, https)?
                builder = builder.uri("http://" + getDockerHostFromEnvironment().get() + ":" + getDockerPortFromEnvironment());
            }

            docker = builder.build();
        } catch (DockerCertificateException e) {
            log.error(e);
        }
    }

    @Override
    public ContainerInspectionResult startContainer(ContainerStartConfiguration configuration) {
        try {
            ContainerConfig config = ContainerConfig.builder().hostname(configuration.getHostname()).image(configuration.getImage()).build();
            ContainerCreation creation = docker.createContainer(config);

            docker.startContainer(creation.id());

            return new ContainerInspectionResultWrapper(docker.inspectContainer(creation.id()));
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    @Override
    public void stopContainer(String containerId) {
        try {
            docker.stopContainer(containerId, 0);
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    public void deleteContainer(String containerId) {
        try {
            docker.removeContainer(containerId);
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    public List<ExposedPort> getExposedPorts(String containerId) {
        List<ExposedPort> ports = new ArrayList<>();
        try {
            ContainerInfo info = docker.inspectContainer(containerId);

            // info.config().exposedPorts();

            for(Map.Entry<String, List<PortBinding>> port : info.networkSettings().ports().entrySet()) {
                if(port!=null && port.getValue()!=null) {
                    for(PortBinding portBinding : port.getValue()) {
                        // TODO: not sure if this is correct
                        ports.add(new ExposedPort(port.getKey(), Integer.valueOf(portBinding.hostPort()), portBinding.hostIp()));
                    }
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
        return ports;
    }

    @Override
    public String buildImage(ImageBuildConfiguration image) {
        try {
            String imageName = image.getNameAndTag();
            // NOTE: Docker fails with slashes; might also be related to Docker 1.4; other provider throw same exception
            //       ... or should we just fail instead of applying some magic?
            int pos = imageName.indexOf("/");
            if(pos!=-1) {
                imageName = imageName.replaceAll("/", "_");
            }
            return docker.build(Paths.get(image.getDockerFile().getParent()), imageName, new ProgressHandler() {
                @Override
                public void progress(ProgressMessage message) throws DockerException {
                    if(!StringUtils.isEmpty(message.error())) {
                        log.error(message.error());
                    }
                    if(!StringUtils.isEmpty(message.stream())) {
                        log.info(message.stream());
                    }
                }
            });
        } catch (Exception e) {
            log.error(e);
        }

        return null;
    }

    @Override
    public void removeImage(String imageId) {
        try {
            docker.removeImage(imageId);
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    public void pushImage(String nameAndTag) {
        try {
            docker.push(nameAndTag);
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    public void tagImage(String imageId, String nameAndTag) {
        try {
            // TODO: this doesn't seem to work
            docker.tag(imageId, nameAndTag);
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    public String getLogs(String containerId) {
        try {
            LogStream stream = docker.logs(containerId, DockerClient.LogsParameter.TIMESTAMPS);
            return stream.readFully();
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    @Override
    public void setLogger(Log logger) {
        this.log = logger;
    }

    private static Integer getDockerPortFromEnvironment() {
        return DockerPortFromPropertySupplier.INSTANCE.get()
                .or(DockerPortFromEnvironmentSupplier.INSTANCE.get())
                .or(DEFAULT_DOCKER_PORT);
    }

    private static Optional<String> getDockerHostFromEnvironment() {
        return DockerHostFromPropertySupplier.INSTANCE.get()
                .or(DockerHostFromEnvironmentSupplier.INSTANCE.get());
    }

    public static class ContainerInspectionResultWrapper extends ContainerInspectionResult {
        private ContainerInfo info;

        private ConfigWrapper config;

        private NetworkSettingsWrapper network;

        public ContainerInspectionResultWrapper(ContainerInfo info) {
            this.info = info;
            this.config = new ConfigWrapper(info.config());
            this.network = new NetworkSettingsWrapper(info.networkSettings());
        }

        @Override
        public String getId() {
            return info.id();
        }

        @Override
        public String getName() {
            return info.name();
        }

        @Override
        public Calendar getCreatedAt() {
            Calendar c = Calendar.getInstance();
            c.setTime(info.created());
            return c;
        }

        @Override
        public String getPath() {
            return info.path();
        }

        @Override
        public List<String> getArgs() {
            return info.args();
        }

        @Override
        public Config getConfig() {
            return config;
        }

        @Override
        public NetworkSettings getNetworkSettings() {
            return network;
        }
    }

    public static class ConfigWrapper extends ContainerInspectionResult.Config {
        private ContainerConfig config;

        public ConfigWrapper(ContainerConfig config) {
            this.config = config;
        }

        @Override
        public String getHostname() {
            return config.hostname();
        }

        @Override
        public String getDomainName() {
            return config.domainname();
        }

        @Override
        public String getUser() {
            return config.user();
        }

        @Override
        public Long getMemory() {
            return config.memory();
        }

        @Override
        public Long getMemorySwap() {
            return config.memorySwap();
        }

        @Override
        public Boolean getAttachStdin() {
            return config.attachStdin();
        }

        @Override
        public Boolean getAttachStdout() {
            return config.attachStdout();
        }

        @Override
        public Boolean getAttachStderr() {
            return config.attachStderr();
        }

        @Override
        public Map<String, Map> getExposedPorts() {
            // TODO: fix this
            //return config.exposedPorts();
            return super.getExposedPorts();
        }

        @Override
        public Boolean getTty() {
            return config.tty();
        }

        @Override
        public Boolean getOpenStdin() {
            return config.openStdin();
        }

        @Override
        public Boolean getStdinOnce() {
            return config.stdinOnce();
        }

        @Override
        public List<String> getEnv() {
            return config.env();
        }

        @Override
        public List<String> getCmd() {
            return config.cmd();
        }

        @Override
        public String getImage() {
            return config.image();
        }

        @Override
        public String getWorkingDir() {
            return config.workingDir();
        }

        @Override
        public List<String> getEntrypoint() {
            return config.entrypoint();
        }
    }

    public static class NetworkSettingsWrapper extends ContainerInspectionResult.NetworkSettings {
        private NetworkSettings network;

        private Map<String, List<PortMappingInfo>> ports;

        public NetworkSettingsWrapper(NetworkSettings network) {
            this.network = network;

            ports = new HashMap<>();

            for(Map.Entry<String, List<PortBinding>> entry: network.ports().entrySet()) {
                if(entry!=null && entry.getValue()!=null) {
                    List<PortMappingInfo> portInfos = new ArrayList<>();
                    for(PortBinding binding : entry.getValue()) {
                        portInfos.add(new PortMappingInfoWrapper(binding));
                    }
                    ports.put(entry.getKey(), portInfos);
                }
            }
        }

        @Override
        public String getBridge() {
            return network.bridge();
        }

        @Override
        public String getGateway() {
            return network.gateway();
        }

        @Override
        public String getIpAddress() {
            return network.ipAddress();
        }

        @Override
        public int getIpPrefixLen() {
            return network.ipPrefixLen();
        }

        @Override
        public Map<String, List<PortMappingInfo>> getPorts() {
            return ports;
        }
    }

    public static class PortMappingInfoWrapper extends ContainerInspectionResult.NetworkSettings.PortMappingInfo {
        private PortBinding binding;

        public PortMappingInfoWrapper(PortBinding binding) {
            this.binding = binding;
        }

        @Override
        public String getHostIp() {
            return binding.hostIp();
        }

        @Override
        public int getHostPort() {
            return Integer.valueOf(binding.hostPort());
        }
    }
}
