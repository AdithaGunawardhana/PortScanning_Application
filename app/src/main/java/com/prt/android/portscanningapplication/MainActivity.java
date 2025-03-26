package com.prt.android.portscanningapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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

        // Request location permission for Wi-Fi SSID
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Search icon click listener
        searchIcon.setOnClickListener(view -> performSearch());

        // Navigation click listeners
        profileImage.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
        settings.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
        scan.setOnClickListener(v -> {
            String ssid = getNetworkSSID();
            String networkType = determineNetworkType(ssid);
            Toast.makeText(this, "Scanning network, please wait...", Toast.LENGTH_SHORT).show();
            performNetworkScan(networkType);
        });
        locations.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LocationActivity.class)));
        info.setOnClickListener(v ->
                Toast.makeText(this, "Info coming soon!", Toast.LENGTH_SHORT).show());
        discover.setOnClickListener(v ->
                Toast.makeText(this, "discovery is coming soon!", Toast.LENGTH_SHORT).show());
        newsletter.setOnClickListener(v ->
                Toast.makeText(this, "Newsletter coming soon!", Toast.LENGTH_SHORT).show());
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
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location permission denied. SSID may not be available.", Toast.LENGTH_SHORT).show();
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
            ssid = "Unknown SSID (Check Location Permission)";
        }
        return ssid;
    }

    private String determineNetworkType(String ssid) {
        if (ssid == null || ssid.equals("Unknown SSID (Check Location Permission)") || ssid.equals("Unknown")) return "Unknown";
        ssid = ssid.toLowerCase();
        if (ssid.contains("uni") || ssid.contains("edu") || ssid.contains("university")) {
            return "University";
        } else if (ssid.contains("guest")) {
            return "Guest";
        } else {
            return "Home";
        }
    }

    private Map<String, String> getNetworkDetails() {
        Map<String, String> networkDetails = new HashMap<>();
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        // Connection Type
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        String connectionType = "Unknown";
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                connectionType = "Wi-Fi";
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                connectionType = "Mobile Data";
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET) {
                connectionType = "Ethernet";
            }
        }
        networkDetails.put("Connection Type", connectionType);

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
                        final int finalPort = port; // Final variable for lambda
                        portScanTasks.add(() -> {
                            try {
                                Socket socket = new Socket();
                                socket.connect(new InetSocketAddress(deviceIp, finalPort), 500);
                                synchronized (deviceResults) {
                                    deviceResults.append("  Port ").append(finalPort).append(" (").append(portServices.get(finalPort))
                                            .append("): Open");
                                    if (portVulnerabilities.containsKey(finalPort)) {
                                        deviceResults.append(" (Warning: ").append(portVulnerabilities.get(finalPort)).append(")");
                                        totalVulnerabilities.incrementAndGet();
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