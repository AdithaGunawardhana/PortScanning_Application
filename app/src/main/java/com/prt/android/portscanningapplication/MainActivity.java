package com.prt.android.portscanningapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private EditText searchIp;
    private ImageView profileImage, settings, scan, locations, info, discover, newsletter, searchIcon;
    private TextView upgrade;
    private FusedLocationProviderClient fusedLocationClient; // Added for location services

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Intent intent = getIntent();
        String email = intent.getStringExtra("email");

        // Initialize UI elements
        searchIp = findViewById(R.id.searchText);
        profileImage = findViewById(R.id.imageProfile);
        settings = findViewById(R.id.btnSettings);
        scan = findViewById(R.id.btnScan);
        locations = findViewById(R.id.btnLocation);
        info = findViewById(R.id.btnInfo);
        discover = findViewById(R.id.btnDiscover);
        newsletter = findViewById(R.id.btnNewsletter);
        upgrade = findViewById(R.id.upgradeAccount);
        searchIcon = findViewById(R.id.searchIcon);

        // Request permissions for Wi-Fi SSID, network state, internet, and location
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.INTERNET
        };
        boolean needsPermissions = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needsPermissions = true;
                break;
            }
        }
        if (needsPermissions) {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Search icon click listener
        searchIcon.setOnClickListener(view -> performSearch());

        // Navigation click listeners
        profileImage.setOnClickListener(v -> {
            Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
            profileIntent.putExtra("email", email);
            startActivity(profileIntent);
        });
        settings.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
        scan.setOnClickListener(v -> {
            Map<String, String> networkDetails = getNetworkDetails();
            String networkType = networkDetails.get("Network Type");
            Toast.makeText(this, "Scanning network, please wait...", Toast.LENGTH_SHORT).show();
            performNetworkScan(networkType);
        });
        locations.setOnClickListener(v -> {
            // Check for location permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            // Get last known location
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Create intent to open Google Maps with current location
                            String uri = String.format("geo:%f,%f?q=%f,%f",
                                    location.getLatitude(), location.getLongitude(),
                                    location.getLatitude(), location.getLongitude());
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                            mapIntent.setPackage("com.google.android.apps.maps");

                            // Verify that Google Maps is installed
                            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(mapIntent);
                            } else {
                                Toast.makeText(this, "Google Maps not installed",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Unable to get location",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Toast.makeText(this, "Error getting location: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });
        info.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AboutActivity.class)));
        discover.setOnClickListener(v ->
                Toast.makeText(this, "Discovery is coming soon!", Toast.LENGTH_SHORT).show());
        newsletter.setOnClickListener(v -> {
            Intent newsletterIntent = new Intent(MainActivity.this, NewsletterActivity.class);
            newsletterIntent.putExtra("email", email);
            startActivity(newsletterIntent);
        });
        upgrade.setOnClickListener(v ->
                Toast.makeText(this, "Upgrade coming soon!", Toast.LENGTH_SHORT).show());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
                // Retry location if permission was just granted
                locations.performClick();
            } else {
                Toast.makeText(this, "Permissions denied. Location and network details may be limited.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void performSearch() {
        String searchAddress = searchIp.getText().toString().trim();
        if (searchAddress.isEmpty()) {
            Toast.makeText(this, "Please enter an IP or port number!", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.putExtra("SEARCH_ADDRESS", searchAddress);
            startActivity(intent);
        }
    }

    private String getNetworkSSID() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        } else if (ssid == null || ssid.equals("<unknown ssid>")) {
            ssid = "Unknown SSID";
        }
        return ssid;
    }

    private Map<String, String> getNetworkDetails() {
        Map<String, String> networkDetails = new HashMap<>();
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        // Connection Type and Network Type
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        String connectionType = "Unknown";
        String networkType = "Unknown";
        if (activeNetwork != null && activeNetwork.isConnected()) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                connectionType = "Wi-Fi";
                String ssid = getNetworkSSID().toLowerCase();
                if (ssid.contains("uni") || ssid.contains("edu") || ssid.contains("university")) {
                    networkType = "University Wi-Fi (" + ssid + ")";
                } else if (ssid.contains("guest")) {
                    networkType = "Guest Wi-Fi (" + ssid + ")";
                } else if (ssid.equals("Unknown SSID")) {
                    networkType = "Unknown Wi-Fi";
                } else {
                    if (ssid.contains("home") || ssid.length() <= 12) {
                        networkType = "Home Wi-Fi (" + ssid + ")";
                    } else {
                        networkType = "Public Wi-Fi (" + ssid + ")";
                    }
                }
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                connectionType = "Mobile Data";
                networkType = "Mobile Data";
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET) {
                connectionType = "Ethernet";
                networkType = "Wired Network";
            }
        }
        networkDetails.put("Connection Type", connectionType);
        networkDetails.put("Network Type", networkType);

        // SSID
        String ssid = getNetworkSSID();
        networkDetails.put("SSID", ssid);

        // Local IP Address
        int ipAddress = wifiInfo.getIpAddress();
        String localIp = String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        networkDetails.put("Local IP", localIp != null && !localIp.equals("0.0.0.0") ? localIp : "Unknown");

        // Gateway IP
        String gatewayIp = intToIp(dhcpInfo.gateway);
        networkDetails.put("Gateway IP", gatewayIp != null && !gatewayIp.equals("0.0.0.0") ? gatewayIp : "Unknown");

        // Subnet Mask
        String subnetMask = intToIp(dhcpInfo.netmask);
        networkDetails.put("Subnet Mask", subnetMask != null && !subnetMask.equals("0.0.0.0") ? subnetMask : "255.255.255.0");

        // DNS Servers
        String dns1 = intToIp(dhcpInfo.dns1);
        String dns2 = intToIp(dhcpInfo.dns2);
        networkDetails.put("DNS 1", dns1 != null && !dns1.equals("0.0.0.0") ? dns1 : "Unknown");
        networkDetails.put("DNS 2", dns2 != null && !dns2.equals("0.0.0.0") ? dns2 : "Unknown");

        return networkDetails;
    }

    private String intToIp(int ip) {
        if (ip == 0) return null;
        return String.format("%d.%d.%d.%d",
                (ip & 0xff), (ip >> 8 & 0xff),
                (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }

    private void performNetworkScan(String networkType) {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        new Thread(() -> {
            Map<String, Object> resultMap = new HashMap<>();
            StringBuilder results = new StringBuilder();
            AtomicInteger totalDevices = new AtomicInteger(0);
            AtomicInteger totalOpenPorts = new AtomicInteger(0);
            AtomicInteger totalVulnerabilities = new AtomicInteger(0);

            // Gather network details
            Map<String, String> networkDetails = getNetworkDetails();
            results.append("Network Details:\n");
            results.append("Connection Type: ").append(networkDetails.get("Connection Type")).append("\n");
            results.append("SSID: ").append(networkDetails.get("SSID")).append("\n");
            results.append("Local IP: ").append(networkDetails.get("Local IP")).append("\n");
            results.append("Gateway IP: ").append(networkDetails.get("Gateway IP")).append("\n");
            results.append("Subnet Mask: ").append(networkDetails.get("Subnet Mask")).append("\n");
            results.append("DNS 1: ").append(networkDetails.get("DNS 1")).append("\n");
            results.append("DNS 2: ").append(networkDetails.get("DNS 2")).append("\n\n");

            try {
                WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int ipAddress = wifiInfo.getIpAddress();
                String localIp = String.format("%d.%d.%d.%d",
                        (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                        (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

                if (localIp.equals("0.0.0.0")) {
                    results.append("Error: Unable to determine local IP. Check network connection.\n");
                    resultMap.put("networkType", networkType);
                    resultMap.put("scanResults", results.toString());
                    runOnUiThread(() -> {
                        Intent intent = new Intent(MainActivity.this, NetworkScanActivity.class);
                        intent.putExtra("NETWORK_TYPE", (String) resultMap.get("networkType"));
                        intent.putExtra("SCAN_RESULTS", (String) resultMap.get("scanResults"));
                        startActivity(intent);
                    });
                    return;
                }

                String baseIp = localIp.substring(0, localIp.lastIndexOf("."));
                List<String> devices = new ArrayList<>();
                Map<String, String> deviceHostnames = new HashMap<>();

                int[] commonPorts = {20, 21, 22, 23, 25, 53, 80, 110, 143, 443, 445, 3389, 8080, 8443};
                Map<Integer, String> portServices = new HashMap<>();
                Map<Integer, String> portVulnerabilities = new HashMap<>();
                Map<Integer, String> portRecommendations = new HashMap<>();
                Map<Integer, String> portRiskLevels = new HashMap<>();
                Map<Integer, String> portEducations = new HashMap<>();

                // Define port services
                portServices.put(20, "FTP (Data)");
                portServices.put(21, "FTP (Control)");
                portServices.put(22, "SSH");
                portServices.put(23, "Telnet");
                portServices.put(25, "SMTP");
                portServices.put(53, "DNS");
                portServices.put(80, "HTTP");
                portServices.put(110, "POP3");
                portServices.put(143, "IMAP");
                portServices.put(443, "HTTPS");
                portServices.put(445, "SMB");
                portServices.put(3389, "RDP");
                portServices.put(8080, "HTTP (Alt)");
                portServices.put(8443, "HTTPS (Alt)");

                // Define vulnerabilities
                portVulnerabilities.put(20, "Unencrypted data transfer; use SFTP instead.");
                portVulnerabilities.put(21, "Unencrypted control channel; use SFTP or FTPS.");
                portVulnerabilities.put(22, "Ensure strong authentication; weak passwords can be brute-forced.");
                portVulnerabilities.put(23, "Telnet is unencrypted; use SSH instead.");
                portVulnerabilities.put(25, "Unencrypted email transfer; enable STARTTLS.");
                portVulnerabilities.put(53, "Potential for DNS spoofing; use DNSSEC.");
                portVulnerabilities.put(80, "Unencrypted; upgrade to HTTPS.");
                portVulnerabilities.put(110, "Unencrypted email access; use POP3S.");
                portVulnerabilities.put(143, "Unencrypted email access; use IMAPS.");
                portVulnerabilities.put(443, "Ensure TLS configuration is secure (e.g., no outdated protocols).");
                portVulnerabilities.put(445, "SMBv1 is vulnerable; ensure SMBv3 with encryption.");
                portVulnerabilities.put(3389, "RDP can be brute-forced; use strong passwords and MFA.");
                portVulnerabilities.put(8080, "Often used for dev servers; may expose admin interfaces.");
                portVulnerabilities.put(8443, "Ensure TLS configuration is secure.");

                // Define actionable recommendations
                portRecommendations.put(20, "1. Disable FTP if not needed.\n2. Switch to SFTP for secure file transfers.\n3. Configure your firewall to block Port 20.");
                portRecommendations.put(21, "1. Disable FTP if not needed.\n2. Use FTPS or SFTP instead.\n3. Block Port 21 on your firewall.");
                portRecommendations.put(22, "1. Use strong, unique passwords.\n2. Enable two-factor authentication (2FA).\n3. Restrict SSH access to specific IP addresses via firewall rules.");
                portRecommendations.put(23, "1. Disable Telnet immediately.\n2. Use SSH for secure remote access.\n3. Block Port 23 on your firewall.");
                portRecommendations.put(25, "1. Enable STARTTLS for SMTP.\n2. Use a secure email gateway.\n3. Block Port 25 if not needed for outgoing email.");
                portRecommendations.put(53, "1. Enable DNSSEC on your DNS server.\n2. Use trusted DNS providers like Cloudflare (1.1.1.1) or Google (8.8.8.8).\n3. Monitor DNS traffic for anomalies.");
                portRecommendations.put(80, "1. Redirect HTTP to HTTPS.\n2. Obtain an SSL/TLS certificate (e.g., via Let's Encrypt).\n3. Block Port 80 on your firewall.");
                portRecommendations.put(110, "1. Switch to POP3S (secure POP3).\n2. Use SSL/TLS for email clients.\n3. Block Port 110 on your firewall.");
                portRecommendations.put(143, "1. Switch to IMAPS (secure IMAP).\n2. Enable SSL/TLS in your email client.\n3. Block Port 143 on your firewall.");
                portRecommendations.put(443, "1. Use modern TLS versions (TLS 1.2 or 1.3).\n2. Disable outdated protocols (e.g., SSLv3, TLS 1.0).\n3. Regularly update your SSL/TLS certificates.");
                portRecommendations.put(445, "1. Disable SMBv1.\n2. Use SMBv3 with encryption.\n3. Block Port 445 on your firewall for external access.");
                portRecommendations.put(3389, "1. Enable Network Level Authentication (NLA).\n2. Use strong passwords and enable MFA.\n3. Restrict RDP access to specific IPs via firewall.");
                portRecommendations.put(8080, "1. Restrict access to Port 8080.\n2. Use authentication for admin interfaces.\n3. Block Port 8080 on your firewall for external access.");
                portRecommendations.put(8443, "1. Use modern TLS versions.\n2. Disable outdated protocols.\n3. Regularly update SSL/TLS certificates.");

                // Define risk levels for visual indicators
                portRiskLevels.put(20, "High");
                portRiskLevels.put(21, "High");
                portRiskLevels.put(22, "Moderate");
                portRiskLevels.put(23, "High");
                portRiskLevels.put(25, "Moderate");
                portRiskLevels.put(53, "Moderate");
                portRiskLevels.put(80, "High");
                portRiskLevels.put(110, "High");
                portRiskLevels.put(143, "High");
                portRiskLevels.put(443, "Low");
                portRiskLevels.put(445, "High");
                portRiskLevels.put(3389, "Moderate");
                portRiskLevels.put(8080, "Moderate");
                portRiskLevels.put(8443, "Low");

                // Define educational content
                portEducations.put(20, "What is FTP (Data)?\nFTP (File Transfer Protocol) Data port is used to transfer files. It's unencrypted, meaning data can be intercepted.\nLearn more: https://www.cloudflare.com/learning/security/glossary/what-is-ftp/");
                portEducations.put(21, "What is FTP (Control)?\nFTP Control port manages file transfer sessions. It's unencrypted and vulnerable to attacks.\nLearn more: https://www.cloudflare.com/learning/security/glossary/what-is-ftp/");
                portEducations.put(22, "What is SSH?\nSSH (Secure Shell) allows secure remote access to devices. Weak passwords can make it vulnerable.\nLearn more: https://www.ssh.com/academy/ssh");
                portEducations.put(23, "What is Telnet?\nTelnet provides remote access but is unencrypted, making it highly insecure.\nLearn more: https://www.cloudflare.com/learning/security/glossary/what-is-telnet/");
                portEducations.put(25, "What is SMTP?\nSMTP (Simple Mail Transfer Protocol) sends emails. Without encryption, emails can be intercepted.\nLearn more: https://www.cloudflare.com/learning/email-security/what-is-smtp/");
                portEducations.put(53, "What is DNS?\nDNS (Domain Name System) translates domain names to IP addresses. Spoofing can redirect you to malicious sites.\nLearn more: https://www.cloudflare.com/learning/dns/what-is-dns/");
                portEducations.put(80, "What is HTTP?\nHTTP (HyperText Transfer Protocol) is used for web traffic but is unencrypted.\nLearn more: https://www.cloudflare.com/learning/ddos/glossary/hypertext-transfer-protocol-http/");
                portEducations.put(110, "What is POP3?\nPOP3 (Post Office Protocol) retrieves emails but is unencrypted by default.\nLearn more: https://www.cloudflare.com/learning/email-security/what-is-pop3/");
                portEducations.put(143, "What is IMAP?\nIMAP (Internet Message Access Protocol) manages emails on a server but is unencrypted by default.\nLearn more: https://www.cloudflare.com/learning/email-security/what-is-imap/");
                portEducations.put(443, "What is HTTPS?\nHTTPS is the secure version of HTTP, using SSL/TLS for encryption.\nLearn more: https://www.cloudflare.com/learning/ssl/what-is-https/");
                portEducations.put(445, "What is SMB?\nSMB (Server Message Block) shares files/printers but older versions are vulnerable.\nLearn more: https://www.samba.org/samba/what_is_smb.html");
                portEducations.put(3389, "What is RDP?\nRDP (Remote Desktop Protocol) allows remote desktop access but can be exploited if not secured.\nLearn more: https://www.microsoft.com/en-us/security/business/security-101/what-is-rdp");
                portEducations.put(8080, "What is HTTP (Alt)?\nPort 8080 is often used for alternate HTTP services, like development servers.\nLearn more: https://www.cloudflare.com/learning/ddos/glossary/hypertext-transfer-protocol-http/");
                portEducations.put(8443, "What is HTTPS (Alt)?\nPort 8443 is an alternate port for HTTPS, often used for secure web services.\nLearn more: https://www.cloudflare.com/learning/ssl/what-is-https/");

                // Scan for devices in the network (parallelized)
                List<Runnable> deviceScanTasks = new ArrayList<>();
                for (int i = 1; i <= 255; i++) {
                    String targetIp = baseIp + "." + i;
                    deviceScanTasks.add(() -> {
                        try {
                            InetAddress address = InetAddress.getByName(targetIp);
                            if (address.isReachable(50)) {
                                synchronized (devices) {
                                    devices.add(targetIp);
                                    try {
                                        String hostname = address.getCanonicalHostName();
                                        if (!hostname.equals(targetIp)) {
                                            deviceHostnames.put(targetIp, hostname);
                                        } else {
                                            deviceHostnames.put(targetIp, "Unknown");
                                        }
                                    } catch (Exception e) {
                                        deviceHostnames.put(targetIp, "Unknown");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Skip unreachable IPs
                        }
                    });
                }

                for (Runnable task : deviceScanTasks) {
                    executor.execute(task);
                }
                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.SECONDS);

                totalDevices.set(devices.size());
                if (devices.isEmpty()) {
                    results.append("No devices found on the network.\n");
                } else {
                    results.append("Devices found: ").append(totalDevices.get()).append("\n\n");
                }

                // Scan ports for each discovered device (parallelized)
                ExecutorService portScanExecutor = Executors.newFixedThreadPool(5);
                for (String deviceIp : devices) {
                    StringBuilder deviceResults = new StringBuilder();
                    deviceResults.append("Device: ").append(deviceIp);
                    if (deviceHostnames.containsKey(deviceIp) && !deviceHostnames.get(deviceIp).equals("Unknown")) {
                        deviceResults.append(" (").append(deviceHostnames.get(deviceIp)).append(")");
                    }
                    deviceResults.append("\n");

                    List<Runnable> portScanTasks = new ArrayList<>();
                    for (int port : commonPorts) {
                        final int finalPort = port;
                        portScanTasks.add(() -> {
                            try {
                                Socket socket = new Socket();
                                socket.connect(new InetSocketAddress(deviceIp, finalPort), 500);
                                synchronized (deviceResults) {
                                    deviceResults.append("  Port ").append(finalPort).append(" (").append(portServices.get(finalPort))
                                            .append("): Open");
                                    if (portRiskLevels.containsKey(finalPort)) {
                                        deviceResults.append(" [Risk: ").append(portRiskLevels.get(finalPort)).append("]");
                                    }
                                    if (portVulnerabilities.containsKey(finalPort)) {
                                        deviceResults.append("\n    Warning: ").append(portVulnerabilities.get(finalPort));
                                        totalVulnerabilities.incrementAndGet();
                                    }
                                    if (portRecommendations.containsKey(finalPort)) {
                                        deviceResults.append("\n    Recommendation:\n").append(portRecommendations.get(finalPort));
                                    }
                                    if (portEducations.containsKey(finalPort)) {
                                        deviceResults.append("\n    Learn More:\n").append(portEducations.get(finalPort));
                                    }
                                    deviceResults.append("\n");
                                    totalOpenPorts.incrementAndGet();
                                }
                                socket.close();
                            } catch (Exception e) {
                                synchronized (deviceResults) {
                                    deviceResults.append("  Port ").append(finalPort).append(" (").append(portServices.get(finalPort))
                                            .append("): Closed\n");
                                }
                            }
                        });
                    }

                    for (Runnable task : portScanTasks) {
                        portScanExecutor.execute(task);
                    }

                    portScanExecutor.shutdown();
                    portScanExecutor.awaitTermination(20, TimeUnit.SECONDS);
                    synchronized (results) {
                        results.append(deviceResults.toString()).append("\n");
                    }
                    portScanExecutor = Executors.newFixedThreadPool(5);
                }

                portScanExecutor.shutdown();

            } catch (Exception e) {
                results.append("Error during scan: ").append(e.getMessage()).append("\n");
            } finally {
                executor.shutdownNow();
            }

            // Add summary at the top
            results.insert(0, "Summary:\n" +
                    "Total Devices Found: " + totalDevices.get() + "\n" +
                    "Total Open Ports: " + totalOpenPorts.get() + "\n" +
                    "Total Vulnerabilities Detected: " + totalVulnerabilities.get() + "\n\n");

            // Pass results to NetworkScanActivity
            resultMap.put("networkType", networkType);
            resultMap.put("scanResults", results.toString());

            runOnUiThread(() -> {
                Intent intent = new Intent(MainActivity.this, NetworkScanActivity.class);
                intent.putExtra("NETWORK_TYPE", (String) resultMap.get("networkType"));
                intent.putExtra("SCAN_RESULTS", (String) resultMap.get("scanResults"));
                startActivity(intent);
            });
        }).start();
    }
}