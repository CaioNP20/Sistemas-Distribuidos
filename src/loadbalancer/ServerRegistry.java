package loadbalancer;

import java.util.*;

public class ServerRegistry {
    private final List<ServerInfo> servers;
    private final Random random;
    
    public ServerRegistry() {
        this.servers = new ArrayList<>();
        this.random = new Random();
        
        // Servidores padrão do projeto
        registerServer(9101);
        registerServer(9102);
        registerServer(9103);
    }
    
    public void registerServer(int port) {
        servers.add(new ServerInfo(port));
    }
    
    public ServerInfo getRandomServer() {
        List<ServerInfo> available = getAvailableServers();
        if (available.isEmpty()) return null;
        return available.get(random.nextInt(available.size()));
    }
    
    public List<ServerInfo> getAllServers() {
        return new ArrayList<>(servers);
    }
    
    public List<ServerInfo> getAvailableServers() {
        List<ServerInfo> available = new ArrayList<>();
        for (ServerInfo server : servers) {
            if (server.isAvailable()) {
                available.add(server);
            }
        }
        return available;
    }
    
    public void markServerUnavailable(int port) {
        for (ServerInfo server : servers) {
            if (server.getPort() == port) {
                server.setAvailable(false);
                    
                break;
            }
        }
    }
    
    public void markServerAvailable(int port) {
        for (ServerInfo server : servers) {
            if (server.getPort() == port) {
                server.setAvailable(true);
                    
                break;
            }
        }
    }
    
    // Classe interna ServerInfo
    public static class ServerInfo {
        private final int port;
        private boolean available;
        
        public ServerInfo(int port) {
            this.port = port;
            this.available = true;
        }
        
        public int getPort() { return port; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
    }
}