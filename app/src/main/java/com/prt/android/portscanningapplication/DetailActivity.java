package com.prt.android.portscanningapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailActivity extends AppCompatActivity {

    private TextView searchDataTextView, timestampTextView, resourceLinksTextView;
    private ProgressBar progressBar;
    private ImageView generateReportButton;
    private RadioGroup searchModeGroup;
    private String searchAddress;
    private boolean isPortMode = true;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Initialize UI elements
        searchDataTextView = findViewById(R.id.searchDataTextView);
        timestampTextView = findViewById(R.id.timestampTextView);
        resourceLinksTextView = findViewById(R.id.resourceLinksTextView);
        progressBar = findViewById(R.id.progressBar);
        generateReportButton = findViewById(R.id.generateReportButton);
        searchModeGroup = findViewById(R.id.searchModeGroup);

        // Get the search address from intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("SEARCH_ADDRESS")) {
            searchAddress = intent.getStringExtra("SEARCH_ADDRESS");
        }

        // Set up mode toggle
        searchModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isPortMode = checkedId == R.id.radioPort;
            if (searchAddress != null) {
                fetchDataAsync(searchAddress);
            }
        });

        // Generate report button
        generateReportButton.setOnClickListener(v -> generateReport());

        if (searchAddress != null && !searchAddress.isEmpty()) {
            fetchDataAsync(searchAddress);
        } else {
            Toast.makeText(this, "No search address provided! Please enter an IP or port.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void fetchDataAsync(String address) {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        executorService.execute(() -> {
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String dataInfo = isPortMode ? checkPortStatus(address) : checkIpStatus(address);
            SpannableString resourceLinks = getClickableResourceLinks(address);

            mainHandler.post(() -> {
                searchDataTextView.setText(isPortMode ? "Port: " + address : "IP: " + address);
                searchDataTextView.append("\n" + dataInfo);
                timestampTextView.setText("Timestamp: " + currentTime);
                resourceLinksTextView.setText(resourceLinks);
                resourceLinksTextView.setMovementMethod(LinkMovementMethod.getInstance());
                progressBar.setVisibility(ProgressBar.GONE);
            });
        });
    }

    private String checkPortStatus(String port) {
        if (!isValidPort(port)) {
            return "Invalid port number! Must be between 0 and 65535.";
        }
        int portNum = Integer.parseInt(port);
        String status = "Unknown";
        String service = getKnownService(portNum);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("google.com", portNum), 2000);
            status = "Open";
        } catch (IOException e) {
            status = "Closed or Filtered";
        }

        return String.format("Service: %s\nStatus: %s\nDetails: %s",
                service, status, getPortDetails(portNum));
    }

    private String checkIpStatus(String ip) {
        try {
            if (!isValidIp(ip)) {
                return "Invalid IP address format! Use format: xxx.xxx.xxx.xxx";
            }

            InetAddress inetAddress = InetAddress.getByName(ip);
            boolean isReachable = inetAddress.isReachable(2000);
            String hostname = inetAddress.getHostName();
            String ipAddress = inetAddress.getHostAddress();

            return String.format("IP: %s\nHostname: %s\nStatus: %s\nResolved IP: %s\nDetails: Basic connectivity check.",
                    ip, hostname, isReachable ? "Reachable" : "Unreachable", ipAddress);
        } catch (IOException e) {
            return "Error scanning IP: " + e.getMessage();
        }
    }

    private boolean isValidPort(String port) {
        try {
            int portNum = Integer.parseInt(port);
            return portNum >= 0 && portNum <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidIp(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) return false;
        for (String octet : octets) {
            try {
                int value = Integer.parseInt(octet);
                if (value < 0 || value > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private String getKnownService(int port) {
        switch (port) {
            case 20: return "FTP Data";
            case 21: return "FTP Control";
            case 22: return "SSH";
            case 23: return "Telnet";
            case 25: return "SMTP";
            case 53: return "DNS";
            case 80: return "HTTP";
            case 110: return "POP3";
            case 143: return "IMAP";
            case 443: return "HTTPS";
            case 3306: return "MySQL";
            case 3389: return "RDP";
            case 8080: return "HTTP Alternate (commonly used for proxies or web servers)";
            default: return "Unknown Service";
        }
    }

    private String getPortDetails(int port) {
        switch (port) {
            case 80: return "Used by web servers for HTTP traffic.";
            case 443: return "Secure HTTP with SSL/TLS.";
            case 22: return "Secure remote access protocol.";
            case 8080: return "Often used as an alternative to port 80 for web servers or proxies.";
            default: return "No detailed information available.";
        }
    }

    private SpannableString getClickableResourceLinks(String address) {
        String linksText;
        if (isPortMode) {
            try {
                int portNum = Integer.parseInt(address);
                switch (portNum) {
                    case 80:
                        linksText = "Resources:\n1. HTTP Basics - https://www.youtube.com/watch?v=0OrmKCB0UrQ\n2. Web Servers - https://www.networkchuck.com";
                        break;
                    case 443:
                        linksText = "Resources:\n1. HTTPS Explained - https://www.youtube.com/watch?v=T4Df5_cojAs\n2. SSL/TLS - https://www.cloudflare.com/learning/ssl";
                        break;
                    case 22:
                        linksText = "Resources:\n1. SSH Tutorial - https://www.youtube.com/watch?v=ORcvSkgdAmo\n2. SSH Guide - https://www.digitalocean.com/community/tutorials/ssh";
                        break;
                    case 8080:
                        linksText = "Resources:\n1. Port 8080 Usage - https://www.youtube.com/watch?v=example8080\n2. Proxy Servers - https://www.cloudflare.com/learning";
                        break;
                    default:
                        linksText = "Resources:\n1. Networking Basics - https://www.youtube.com/watch?v=example7";
                }
            } catch (NumberFormatException e) {
                linksText = "Resources:\n1. Networking Basics - https://www.youtube.com/watch?v=example7";
            }
        } else {
            linksText = "Resources:\n1. IP Basics - https://www.youtube.com/watch?v=example8\n2. Networking Guide - https://www.cisco.com/c/en/us/support/docs/ip/routing-information-protocol-rip/13788-3.html";
        }

        SpannableString spannableString = new SpannableString(linksText);
        String[] lines = linksText.split("\n");
        for (String line : lines) {
            if (line.contains("http")) {
                int start = linksText.indexOf("http", linksText.indexOf(line));
                int end = line.length() + linksText.indexOf(line);
                String url = line.substring(line.indexOf("http"), line.length());
                spannableString.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    }
                }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return spannableString;
    }

    private void generateReport() {
        String reportContent = searchDataTextView.getText().toString() + "\n" +
                timestampTextView.getText().toString() + "\n" +
                resourceLinksTextView.getText().toString().replaceAll(" - http[^\\n]*", "");

        String fileName = (isPortMode ? "Port_" : "IP_") + searchAddress + "_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";

        try {
            File reportFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            FileWriter writer = new FileWriter(reportFile);
            writer.write(reportContent);
            writer.close();
            Toast.makeText(this, "Report saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving report: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}