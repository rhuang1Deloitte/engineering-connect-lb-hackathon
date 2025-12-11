package com.hackathon.lb.model;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.URISyntaxException;

import com.hackathon.lb.config.HealthCheckConfig;
import com.hackathon.lb.config.TargetConfig;
import com.hackathon.lb.config.TargetGroupConfig;

import io.quarkus.logging.Log;

/**
 * Factory for constructing TargetGroup and Target instances from configuration.
 * Handles the conversion from Quarkus config interfaces to runtime model objects.
 */
public class TargetGroupFactory {
    
    private static final int DEFAULT_WEIGHT = 1;
    
    /**
     * Creates a TargetGroup from configuration with a specified default algorithm.
     * 
     * @param config the target group configuration
     * @param defaultAlgorithm the default algorithm to use if not specified in config
     * @return a new TargetGroup instance
     */
    public static TargetGroup fromConfig(TargetGroupConfig config, String defaultAlgorithm) {
        if (config == null) {
            throw new IllegalArgumentException("TargetGroupConfig cannot be null");
        }

        String algorithm = config.algorithm().orElse(defaultAlgorithm);

        List<Target> targets = new ArrayList<>();
        for (TargetConfig targetConfig : config.targets()) {
            List<Target> resolvedTargets = resolveTargetWithDNS(targetConfig);
            targets.addAll(resolvedTargets);
        }

        if (targets.isEmpty()) {
            throw new IllegalArgumentException("TargetGroup must have at least one target");
        }

        TargetGroup.HealthCheck healthCheck = config.healthCheck()
            .map(TargetGroupFactory::createHealthCheck)
            .orElse(null);

        String pathRewrite = config.pathRewrite().orElse(null);

        return new TargetGroup(config.path(), algorithm, targets, healthCheck, pathRewrite);
    }
    
    /**
     * Creates a HealthCheck from configuration.
     * 
     * @param config the health check configuration
     * @return a new HealthCheck instance
     */
    private static TargetGroup.HealthCheck createHealthCheck(HealthCheckConfig config) {
        if (config == null) {
            return null;
        }
        
        return new TargetGroup.HealthCheck(
            config.enabled(),
            config.path(),
            config.interval(),
            config.successThreshold(),
            config.failureThreshold()
        );
    }
    
    /**
     * Resolves a target configuration with DNS resolution to IPv4 addresses.
     * Returns a list of Target objects, one for each resolved IPv4 address.
     * If DNS resolution fails or no hostname is present, falls back to the original URL.
     * 
     * @param targetConfig the target configuration to resolve
     * @return a list of resolved Target objects
     */
    private static List<Target> resolveTargetWithDNS(TargetConfig targetConfig) {
        List<Target> resolvedTargets = new ArrayList<>();
        String url = targetConfig.url();
        int weight = targetConfig.weight().orElse(DEFAULT_WEIGHT);
        
        try {
            URI uri = parseURI(url);
            String host = uri.getHost();
            
            if (host == null) {
                // Not a hostname, just add as-is
                Log.debug("No hostname found in URL: " + url + ". Using as-is.");
                resolvedTargets.add(new Target(url, weight));
                return resolvedTargets;
            }
            
            List<String> ipv4Addresses = resolveIPv4Addresses(host);
            if (ipv4Addresses.isEmpty()) {
                // No IPv4 found, fallback to original
                Log.debug("No IPv4 addresses resolved for hostname: " + host + ". Using original URL: " + url);
                resolvedTargets.add(new Target(url, weight));
            } else {
                // Create a target for each resolved IPv4
                Log.debug("Resolved hostname " + host + " to " + ipv4Addresses.size() + " IPv4 address(es): " + ipv4Addresses);
                for (String ip : ipv4Addresses) {
                    String resolvedUrl = rebuildURLWithIP(uri, ip);
                    resolvedTargets.add(new Target(resolvedUrl, weight));
                    Log.info("Created target from config URL " + url + " -> " + resolvedUrl);
                }
            }
        } catch (URISyntaxException | UnknownHostException e) {
            // If resolution fails, fallback to original
            Log.info("Failed to resolve target URL " + url + ": " + e.getMessage() + ". Using original URL.");
            resolvedTargets.add(new Target(url, weight));
        }
        
        return resolvedTargets;
    }
    
    /**
     * Parses a URL string into a URI object.
     * 
     * @param url the URL string to parse
     * @return the parsed URI
     * @throws URISyntaxException if the URL is malformed
     */
    private static URI parseURI(String url) throws URISyntaxException {
        return new URI(url);
    }
    
    /**
     * Resolves a hostname to a list of IPv4 addresses.
     * Only IPv4 addresses are returned (not IPv6).
     * 
     * @param host the hostname to resolve
     * @return a list of IPv4 addresses, or empty list if none found
     * @throws UnknownHostException if the hostname cannot be resolved
     */
    private static List<String> resolveIPv4Addresses(String host) throws UnknownHostException {
        List<String> ipv4Addresses = new ArrayList<>();
        InetAddress[] addresses = InetAddress.getAllByName(host);
        
        for (InetAddress addr : addresses) {
            // IPv4 addresses have a 4-byte representation
            if (addr.getAddress().length == 4) {
                ipv4Addresses.add(addr.getHostAddress());
            }
        }
        
        return ipv4Addresses;
    }
    
    /**
     * Rebuilds a URL with an IP address in place of the hostname.
     * 
     * @param uri the original URI
     * @param ip the IPv4 address to use
     * @return the rebuilt URL string
     * @throws URISyntaxException if the URL cannot be rebuilt
     */
    private static String rebuildURLWithIP(URI uri, String ip) throws URISyntaxException {
        return new URI(
            uri.getScheme(),
            uri.getUserInfo(),
            ip,
            uri.getPort(),
            uri.getPath(),
            uri.getQuery(),
            uri.getFragment()
        ).toString();
    }
}
