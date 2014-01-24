package at.ac.tuwien.dsg.mela.dataservice.dataSource.model;

/**
 * Created by omoser on 1/17/14.
 */
public class GangliaDatasourceConfiguration {

    private String host;

    private int port;

    private int pollingIntervalMs;

    public GangliaDatasourceConfiguration() {
    }

    public GangliaDatasourceConfiguration withHost(final String host) {
        this.host = host;
        return this;
    }

    public GangliaDatasourceConfiguration withPort(final int port) {
        this.port = port;
        return this;
    }

    public GangliaDatasourceConfiguration withPollingIntervalMs(final int pollingIntervalMs) {
        this.pollingIntervalMs = pollingIntervalMs;
        return this;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPollingIntervalMs() {
        return pollingIntervalMs;
    }

    public void setPollingIntervalMs(int pollingIntervalMs) {
        this.pollingIntervalMs = pollingIntervalMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GangliaDatasourceConfiguration)) return false;

        GangliaDatasourceConfiguration that = (GangliaDatasourceConfiguration) o;

        if (pollingIntervalMs != that.pollingIntervalMs) return false;
        if (port != that.port) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        result = 31 * result + pollingIntervalMs;
        return result;
    }

    @Override
    public String toString() {
        return "GangliaDatasourceConfiguration{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", pollingIntervalMs=" + pollingIntervalMs +
                '}';
    }
}
