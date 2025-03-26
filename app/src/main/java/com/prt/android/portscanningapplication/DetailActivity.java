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
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailActivity extends AppCompatActivity {

    private TextView searchDataTextView, timestampTextView, resourceLinksTextView;
    private ProgressBar progressBar;
    private RadioGroup searchModeGroup;
    private String searchAddress;
    private boolean isPortMode = true;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        searchDataTextView = findViewById(R.id.searchDataTextView);
        timestampTextView = findViewById(R.id.timestampTextView);
        resourceLinksTextView = findViewById(R.id.resourceLinksTextView);
        progressBar = findViewById(R.id.progressBar);
        searchModeGroup = findViewById(R.id.searchModeGroup);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("SEARCH_ADDRESS")) {
            searchAddress = intent.getStringExtra("SEARCH_ADDRESS");
        }

        searchModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isPortMode = checkedId == R.id.radioPort;
            if (searchAddress != null) {
                fetchDataAsync(searchAddress);
            }
        });

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
        StringBuilder result = new StringBuilder();
        String service = getKnownService(portNum);
        long startTime = System.currentTimeMillis();
        String targetHost = "scanme.nmap.org"; // A public host that allows scanning for testing

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(targetHost, portNum), 2000);
            long latency = System.currentTimeMillis() - startTime;
            result.append("Status: Open\n");
            result.append("Service: ").append(service).append("\n");
            result.append("Latency: ").append(latency).append(" ms\n");
            result.append("Host Tested: ").append(targetHost).append("\n");
            result.append("Details: Port is accepting connections. ").append(getPortDetails(portNum));
        } catch (SocketTimeoutException e) {
            result.append("Status: Closed or Filtered\n");
            result.append("Service: ").append(service).append("\n");
            result.append("Details: Connection timed out. Port may be filtered by a firewall or not in use.\n");
        } catch (IOException e) {
            result.append("Status: Unreachable\n");
            result.append("Service: ").append(service).append("\n");
            result.append("Details: ").append(e.getMessage()).append("\n");
        }
        return result.toString();
    }

    private String checkIpStatus(String ip) {
        StringBuilder result = new StringBuilder();
        try {
            if (!isValidIp(ip)) {
                return "Invalid IP address format! Use format: xxx.xxx.xxx.xxx";
            }

            InetAddress inetAddress = InetAddress.getByName(ip);
            long startTime = System.currentTimeMillis();
            boolean isReachable = inetAddress.isReachable(2000);
            long latency = System.currentTimeMillis() - startTime;

            result.append("IP: ").append(ip).append("\n");
            result.append("Hostname: ").append(inetAddress.getHostName()).append("\n");
            result.append("Status: ").append(isReachable ? "Reachable" : "Unreachable").append("\n");
            result.append("Resolved IP: ").append(inetAddress.getHostAddress()).append("\n");
            result.append("Latency: ").append(latency).append(" ms\n");

            // Additional real-time info
            if (isReachable) {
                result.append("Details: Host responded to ICMP ping. Likely online and accessible.\n");
            } else {
                result.append("Details: No response to ICMP ping. Host may be offline or blocking pings.\n");
            }

            // Check if it's a private IP
            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                result.append("Network Type: Private (LAN)\n");
            } else {
                result.append("Network Type: Public (WAN)\n");
            }
        } catch (IOException e) {
            result.append("Error scanning IP: ").append(e.getMessage());
        }
        return result.toString();
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
            case 445: return "SMB";
            case 3306: return "MySQL";
            case 3389: return "RDP";
            case 5432: return "PostgreSQL";
            case 8080: return "HTTP Alternate";
            default: return "Unknown Service";
        }
    }

    private String getPortDetails(int port) {
        switch (port) {
            case 20: return "Used for FTP data transfer.";
            case 21: return "FTP control port for commands.";
            case 22: return "Secure Shell for encrypted remote access.";
            case 23: return "Telnet for unencrypted remote access (insecure).";
            case 25: return "Simple Mail Transfer Protocol for email.";
            case 53: return "Domain Name System for DNS resolution.";
            case 80: return "HyperText Transfer Protocol for web traffic.";
            case 110: return "Post Office Protocol v3 for email retrieval.";
            case 143: return "Internet Message Access Protocol for email.";
            case 443: return "HTTP Secure with SSL/TLS encryption.";
            case 445: return "Server Message Block for file sharing.";
            case 3306: return "Default MySQL database port.";
            case 3389: return "Remote Desktop Protocol for Windows.";
            case 5432: return "Default PostgreSQL database port.";
            case 8080: return "Commonly used for web proxies or alternate web servers.";
            default: return "No specific details available.";
        }
    }

    private SpannableString getClickableResourceLinks(String address) {
        String linksText;
        if (isPortMode) {
            try {
                int portNum = Integer.parseInt(address);
                switch (portNum) {
                    case 20:
                    case 21:
                        linksText = "Resources:\n" +
                                "1. FTP Tutorial - https://www.youtube.com/watch?v=tvW83Flr0mM\n" +
                                "2. FTP Docs - https://datatracker.ietf.org/doc/html/rfc959\n" +
                                "3. FTP Course - https://www.udemy.com/course/ftp-for-beginners/";
                        break;
                    case 22:
                        linksText = "Resources:\n" +
                                "1. SSH Basics - https://www.youtube.com/watch?v=ORcvSkgdAmo\n" +
                                "2. SSH Guide - https://www.digitalocean.com/community/tutorials/how-to-use-ssh\n" +
                                "3. SSH Course - https://www.pluralsight.com/courses/ssh-securing-linux-command-line";
                        break;
                    case 25:
                        linksText = "Resources:\n" +
                                "1. SMTP Explained - https://www.youtube.com/watch?v=1QhWvNHMhVc\n" +
                                "2. SMTP RFC - https://datatracker.ietf.org/doc/html/rfc5321\n" +
                                "3. Email Protocols Course - https://www.linkedin.com/learning/email-protocols";
                        break;
                    case 53:
                        linksText = "Resources:\n" +
                                "1. DNS Basics - https://www.youtube.com/watch?v=7lxgpwAKLvc\n" +
                                "2. DNS Docs - https://www.cloudflare.com/learning/dns/what-is-dns/\n" +
                                "3. DNS Course - https://www.coursera.org/learn/dns-fundamentals";
                        break;
                    case 80:
                        linksText = "Resources:\n" +
                                "1. HTTP Basics - https://www.youtube.com/watch?v=0OrmKCB0UrQ\n" +
                                "2. HTTP Docs - https://developer.mozilla.org/en-US/docs/Web/HTTP\n" +
                                "3. Web Dev Course - https://www.udemy.com/course/the-complete-web-development-bootcamp/";
                        break;
                    case 443:
                        linksText = "Resources:\n" +
                                "1. HTTPS Explained - https://www.youtube.com/watch?v=T4Df5_cojAs\n" +
                                "2. SSL/TLS Guide - https://www.cloudflare.com/learning/ssl/what-is-ssl/\n" +
                                "3. Security Course - https://www.pluralsight.com/courses/ssl-tls-fundamentals";
                        break;
                    case 445:
                        linksText = "Resources:\n" +
                                "1. SMB Tutorial - https://www.youtube.com/watch?v=5z5nmgK5lsk\n" +
                                "2. SMB Docs - https://docs.microsoft.com/en-us/windows-server/storage/file-server/file-server-smb\n" +
                                "3. Networking Course - https://www.udemy.com/course/networking-fundamentals/";
                        break;
                    case 3306:
                        linksText = "Resources:\n" +
                                "1. MySQL Basics - https://www.youtube.com/watch?v=7S_tz1z_5bA\n" +
                                "2. MySQL Docs - https://dev.mysql.com/doc/\n" +
                                "3. MySQL Course - https://www.coursera.org/learn/mysql-databases-for-beginners";
                        break;
                    case 3389:
                        linksText = "Resources:\n" +
                                "1. RDP Tutorial - https://www.youtube.com/watch?v=5ZxDahvX5Ec\n" +
                                "2. RDP Docs - https://docs.microsoft.com/en-us/windows-server/remote/remote-desktop-services/\n" +
                                "3. Windows Admin Course - https://www.pluralsight.com/courses/windows-server-administration-fundamentals";
                        break;
                    case 5432:
                        linksText = "Resources:\n" +
                                "1. PostgreSQL Basics - https://www.youtube.com/watch?v=qw--VYLpxG4\n" +
                                "2. PostgreSQL Docs - https://www.postgresql.org/docs/\n" +
                                "3. PostgreSQL Course - https://www.udemy.com/course/postgresql-mastery/";
                        break;
                    case 8080:
                        linksText = "Resources:\n" +
                                "1. Proxy Basics - https://www.youtube.com/watch?v=5c-hCfqi1sM\n" +
                                "2. HTTP Proxy Guide - https://www.cloudflare.com/learning/cdn/what-is-a-proxy-server/\n" +
                                "3. Networking Course - https://www.linkedin.com/learning/networking-foundations-networking-basics";
                        break;
                    default:
                        linksText = "Resources:\n" +
                                "1. Networking Basics - https://www.youtube.com/watch?v=qiQR5rTSshw\n" +
                                "2. Port Reference - https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml\n" +
                                "3. Networking Course - https://www.coursera.org/learn/computer-networking";
                }
            } catch (NumberFormatException e) {
                linksText = "Resources:\n" +
                        "1. Networking Basics - https://www.youtube.com/watch?v=qiQR5rTSshw\n" +
                        "2. General Guide - https://www.cisco.com/c/en/us/support/docs/ip/routing-information-protocol-rip/13788-3.html";
            }
        } else {
            linksText = "Resources:\n" +
                    "1. IP Basics - https://www.youtube.com/watch?v=thhG-P0-88k\n" +
                    "2. IP Addressing Guide - https://www.cisco.com/c/en/us/support/docs/ip/routing-information-protocol-rip/13788-3.html\n" +
                    "3. Networking Course - https://www.udemy.com/course/complete-networking-fundamentals-course-ccna-start/\n" +
                    "4. Subnetting Tutorial - https://www.youtube.com/watch?v=5WIFMz4AroE\n" +
                    "5. IP Tools - https://www.whois.com/whois/";
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