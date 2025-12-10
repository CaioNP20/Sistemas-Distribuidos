package main;

import server.Server;
import loadbalancer.LoadBalancer;
import client.Client;

public class Main {
    public static void main(String[] args) throws Exception {
        
        System.out.println("==========================================");
        System.out.println("   DISTRIBUTED SYSTEM - STARTING");
        System.out.println("==========================================");
        
        // Start the 3 servers in separate threads
        System.out.println("\n[STEP 1] Initializing application servers...");
        
        Thread server1 = new Thread(() -> {
            try {
                System.out.println("  • Server 9101: Starting...");
                Server.main(new String[]{"9101"});
            } catch (Exception e) {
                System.err.println("  • Server 9101: FAILED - " + e.getMessage());
            }
        });
        
        Thread server2 = new Thread(() -> {
            try {
                System.out.println("  • Server 9102: Starting...");
                Server.main(new String[]{"9102"});
            } catch (Exception e) {
                System.err.println("  • Server 9102: FAILED - " + e.getMessage());
            }
        });
        
        Thread server3 = new Thread(() -> {
            try {
                System.out.println("  • Server 9103: Starting...");
                Server.main(new String[]{"9103"});
            } catch (Exception e) {
                System.err.println("  • Server 9103: FAILED - " + e.getMessage());
            }
        });
        
        // Start servers with delay
        server1.start();
        Thread.sleep(300);
        server2.start();
        Thread.sleep(300);
        server3.start();
        Thread.sleep(1500); // Wait for servers to initialize
        
        // Start load balancer
        System.out.println("\n[STEP 2] Initializing load balancer...");
        Thread balancer = new Thread(() -> {
            try {
                System.out.println("  • Load Balancer: Starting on port 9000...");
                LoadBalancer.main(new String[]{});
            } catch (Exception e) {
                System.err.println("  • Load Balancer: FAILED - " + e.getMessage());
            }
        });
        balancer.start();
        Thread.sleep(1000); // Wait for load balancer
        
        // Start client
        System.out.println("\n[STEP 3] Initializing client...");
        System.out.println("==========================================");
        System.out.println("   SYSTEM OPERATIONAL");
        System.out.println("   Client is now sending requests...");
        System.out.println("==========================================");
        
        Client.main(new String[]{});
    }
}