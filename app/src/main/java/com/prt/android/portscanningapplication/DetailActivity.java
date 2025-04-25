package com.prt.android.portscanningapplication;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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

    // Port-related data maps from MainActivity
    private final Map<Integer, String> portServices = new HashMap<>();
    private final Map<Integer, String> portVulnerabilities = new HashMap<>();
    private final Map<Integer, String> portRecommendations = new HashMap<>();
    private final Map<Integer, String> portRiskLevels = new HashMap<>();
    private final Map<Integer, String> portEducations = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Initialize UI components
        searchDataTextView = findViewById(R.id.searchDataTextView);
        timestampTextView = findViewById(R.id.timestampTextView);
        resourceLinksTextView = findViewById(R.id.resourceLinksTextView);
        progressBar = findViewById(R.id.progressBar);
        searchModeGroup = findViewById(R.id.searchModeGroup);

        // Initialize port data
        initializePortData();

        // Get search address from intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("SEARCH_ADDRESS")) {
            searchAddress = intent.getStringExtra("SEARCH_ADDRESS");
        }

        // Set radio group listener
        searchModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isPortMode = checkedId == R.id.radioPort;
            if (searchAddress != null) {
                fetchDataAsync(searchAddress);
            }
        });

        // Start data fetch if address is provided
        if (searchAddress != null && !searchAddress.isEmpty()) {
            fetchDataAsync(searchAddress);
        } else {
            Toast.makeText(this, "No search address provided! Please enter an IP or port.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializePortData() {
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
        portServices.put(3306, "MySQL");
        portServices.put(5432, "PostgreSQL");

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
        portVulnerabilities.put(3306, "Unsecured MySQL instances can expose sensitive data.");
        portVulnerabilities.put(5432, "Unsecured PostgreSQL instances can expose sensitive data.");

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
        portRecommendations.put(3306, "1. Restrict MySQL access to specific IPs.\n2. Use strong passwords.\n3. Enable SSL/TLS for MySQL connections.");
        portRecommendations.put(5432, "1. Restrict PostgreSQL access to specific IPs.\n2. Use strong passwords.\n3. Enable SSL/TLS for PostgreSQL connections.");

        // Define risk levels
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
        portRiskLevels.put(3306, "High");
        portRiskLevels.put(5432, "High");

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
        portEducations.put(3306, "What is MySQL?\nMySQL is a database management system. Unsecured instances can expose sensitive data.\nLearn more: https://dev.mysql.com/doc/");
        portEducations.put(5432, "What is PostgreSQL?\nPostgreSQL is a powerful database system. Unsecured instances can expose sensitive data.\nLearn more: https://www.postgresql.org/docs/");
    }

    private void fetchDataAsync(String address) {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        executorService.execute(() -> {
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            SpannableString dataInfo = isPortMode ? checkPortStatus(address) : checkIpStatus(address);
            SpannableString resourceLinks = getClickableResourceLinks(address);

            mainHandler.post(() -> {
                searchDataTextView.setText(isPortMode ? "Port: " + address : "IP: " + address);
                searchDataTextView.append("\n");
                searchDataTextView.append(dataInfo);
                timestampTextView.setText("Timestamp: " + currentTime);
                resourceLinksTextView.setText(resourceLinks);
                resourceLinksTextView.setMovementMethod(LinkMovementMethod.getInstance());
                progressBar.setVisibility(ProgressBar.GONE);
            });
        });
    }

    private SpannableString checkPortStatus(String port) {
        StringBuilder result = new StringBuilder();
        if (!isValidPort(port)) {
            return new SpannableString("Invalid port number! Must be between 0 and 65535.");
        }
        int portNum = Integer.parseInt(port);
        long startTime = System.currentTimeMillis();
        String targetHost = "scanme.nmap.org";

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(targetHost, portNum), 2000);
            long latency = System.currentTimeMillis() - startTime;
            result.append("Status: Open\n");
            result.append("Service: ").append(portServices.getOrDefault(portNum, "Unknown Service")).append("\n");
            result.append("Latency: ").append(latency).append(" ms\n");
            result.append("Host Tested: ").append(targetHost).append("\n");
            result.append("Details: Port is accepting connections. ").append(getPortDetails(portNum)).append("\n");
            if (portVulnerabilities.containsKey(portNum)) {
                result.append("Warning: ").append(portVulnerabilities.get(portNum)).append("\n");
            }
            if (portRecommendations.containsKey(portNum)) {
                result.append("Recommendation:\n").append(portRecommendations.get(portNum)).append("\n");
            }
            if (portEducations.containsKey(portNum)) {
                result.append("Learn More:\n").append(portEducations.get(portNum).replaceAll("https?://[^\\s]+", "")).append("\n");
            }
        } catch (SocketTimeoutException e) {
            result.append("Status: Closed or Filtered\n");
            result.append("Service: ").append(portServices.getOrDefault(portNum, "Unknown Service")).append("\n");
            result.append("Details: Connection timed out. Port may be filtered by a firewall or not in use.\n");
        } catch (IOException e) {
            result.append("Status: Unreachable\n");
            result.append("Service: ").append(portServices.getOrDefault(portNum, "Unknown Service")).append("\n");
            result.append("Details: ").append(e.getMessage()).append("\n");
        }

        // Add color-coded risk level
        String riskLevel = portRiskLevels.getOrDefault(portNum, "Unknown");
        result.append("Risk Level: ").append(riskLevel).append("\n");
        SpannableString spannableResult = new SpannableString(result.toString());
        int riskStart = result.indexOf("Risk Level: ") + "Risk Level: ".length();
        int riskEnd = riskStart + riskLevel.length();

        // Apply color based on risk level
        int color;
        switch (riskLevel) {
            case "High":
                color = Color.RED;
                break;
            case "Moderate":
                color = Color.parseColor("#FFA500"); // Orange
                break;
            case "Low":
                color = Color.GREEN;
                break;
            default:
                color = Color.BLACK;
        }
        spannableResult.setSpan(new ForegroundColorSpan(color), riskStart, riskEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableResult;
    }

    private SpannableString checkIpStatus(String ip) {
        StringBuilder result = new StringBuilder();
        try {
            if (!isValidIp(ip)) {
                return new SpannableString("Invalid IP address format! Use format: xxx.xxx.xxx.xxx");
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

            String networkType = (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172."))
                    ? "Private (LAN)" : "Public (WAN)";
            result.append("Network Type: ").append(networkType).append("\n");

            String riskLevel = isReachable ? (networkType.equals("Public (WAN)") ? "Moderate" : "Low") : "Unknown";
            result.append("Risk Level: ").append(riskLevel).append("\n");

            if (isReachable) {
                result.append("Details: Host responded to ICMP ping. Likely online and accessible.\n");
            } else {
                result.append("Details: No response to ICMP ping. Host may be offline or blocking pings.\n");
            }

            // Add recommendations for IP
            if (networkType.equals("Public (WAN)")) {
                result.append("Recommendation:\n1. Ensure firewalls are configured to block unnecessary ports.\n2. Use VPNs for secure communication.\n3. Regularly monitor for unauthorized access.\n");
            } else {
                result.append("Recommendation:\n1. Secure your LAN with strong Wi-Fi passwords.\n2. Disable unused services.\n3. Update router firmware regularly.\n");
            }

            SpannableString spannableResult = new SpannableString(result.toString());
            int riskStart = result.indexOf("Risk Level: ") + "Risk Level: ".length();
            int riskEnd = riskStart + riskLevel.length();

            // Apply color based on risk level
            int color;
            switch (riskLevel) {
                case "Moderate":
                    color = Color.parseColor("#FFA500"); // Orange
                    break;
                case "Low":
                    color = Color.GREEN;
                    break;
                default:
                    color = Color.BLACK;
            }
            spannableResult.setSpan(new ForegroundColorSpan(color), riskStart, riskEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            return spannableResult;
        } catch (IOException e) {
            return new SpannableString("Error scanning IP: " + e.getMessage());
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
            case 8443: return "Alternate port for HTTPS, often used for secure web services.";
            default: return "No specific details available.";
        }
    }

    private SpannableString getClickableResourceLinks(String address) {
        StringBuilder linksText = new StringBuilder("Resources:\n");
        if (isPortMode) {
            try {
                int portNum = Integer.parseInt(address);
                switch (portNum) {
                    case 20:
                    case 21:
                        linksText.append("1. FTP Tutorial - https://www.youtube.com/watch?v=tvW83Flr0mM\n")
                                .append("2. FTP Docs - https://datatracker.ietf.org/doc/html/rfc959\n")
                                .append("3. FTP Course - https://www.udemy.com/course/ftp-for-beginners/\n")
                                .append("4. Secure FTP Guide - https://www.cloudflare.com/learning/security/glossary/what-is-ftp/\n")
                                .append("5. FTP Security - https://owasp.org/www-community/vulnerabilities/FTP_Unencrypted_Transmission\n");
                        break;
                    case 22:
                        linksText.append("1. SSH Basics - https://www.youtube.com/watch?v=ORcvSkgdAmo\n")
                                .append("2. SSH Guide - https://www.digitalocean.com/community/tutorials/how-to-use-ssh\n")
                                .append("3. SSH Course - https://www.pluralsight.com/courses/ssh-securing-linux-command-line\n")
                                .append("4. SSH Security - https://www.ssh.com/academy/ssh/security\n")
                                .append("5. SSH Hardening - https://www.cyberciti.biz/tips/linux-unix-bsd-openssh-server-best-practices.html\n");
                        break;
                    case 25:
                        linksText.append("1. SMTP Explained - https://www.youtube.com/watch?v=1QhWvNHMhVc\n")
                                .append("2. SMTP RFC - https://datatracker.ietf.org/doc/html/rfc5321\n")
                                .append("3. Email Protocols Course - https://www.linkedin.com/learning/email-protocols\n")
                                .append("4. SMTP Security - https://www.cloudflare.com/learning/email-security/what-is-smtp/\n")
                                .append("5. Email Security Guide - https://www.cisa.gov/secure-email\n");
                        break;
                    case 53:
                        linksText.append("1. DNS Basics - https://www.youtube.com/watch?v=7lxgpwAKLvc\n")
                                .append("2. DNS Docs - https://www.cloudflare.com/learning/dns/what-is-dns/\n")
                                .append("3. DNS Course - https://www.coursera.org/learn/dns-fundamentals\n")
                                .append("4. DNS Security - https://www.cloudflare.com/learning/dns/dnssec/\n")
                                .append("5. DNS Best Practices - https://www.cisecurity.org/benchmark/dns/\n");
                        break;
                    case 80:
                        linksText.append("1. HTTP Basics - https://www.youtube.com/watch?v=0OrmKCB0UrQ\n")
                                .append("2. HTTP Docs - https://developer.mozilla.org/en-US/docs/Web/HTTP\n")
                                .append("3. Web Dev Course - https://www.udemy.com/course/the-complete-web-development-bootcamp/\n")
                                .append("4. HTTP Security - https://owasp.org/www-project-secure-headers/\n")
                                .append("5. Web Security Guide - https://www.cloudflare.com/learning/security/threats/web-attacks/\n");
                        break;
                    case 443:
                        linksText.append("1. HTTPS Explained - https://www.youtube.com/watch?v=T4Df5_cojAs\n")
                                .append("2. SSL/TLS Guide - https://www.cloudflare.com/learning/ssl/what-is-ssl/\n")
                                .append("3. Security Course - https://www.pluralsight.com/courses/ssl-tls-fundamentals\n")
                                .append("4. TLS Best Practices - https://www.ssllabs.com/projects/best-practices/\n")
                                .append("5. SSL/TLS Tutorial - https://www.digicert.com/what-is-ssl-tls-and-https\n");
                        break;
                    case 445:
                        linksText.append("1. SMB Tutorial - https://www.youtube.com/watch?v=5z5nmgK5lsk\n")
                                .append("2. SMB Docs - https://docs.microsoft.com/en-us/windows-server/storage/file-server/file-server-smb\n")
                                .append("3. Networking Course - https://www.udemy.com/course/networking-fundamentals/\n")
                                .append("2. MySQL Docs - https://dev.mysql.com/doc/\n")
                            .append("3. MySQL Course - https://www.coursera.org/learn/mysql-databases-for-beginners\n")
                            .append("4. MySQL Security - https://dev.mysql.com/doc/refman/8.0/en/security.html\n")
                            .append("5. Database Security Guide - https://owasp.org/www-project-top-ten/\n");
                        break;
                    case 3389:
                        linksText.append("1. RDP Tutorial - https://www.youtube.com/watch?v=5ZxDahvX5Ec\n")
                                .append("2. RDP Docs - https://docs.microsoft.com/en-us/windows-server/remote/remote-desktop-services/\n")
                                .append("3. Windows Admin Course - https://www.pluralsight.com/courses/windows-server-administration-fundamentals\n")
                                .append("4. RDP Security - https://www.cisa.gov/uscert/ncas/tips/ST18-283\n")
                                .append("5. Remote Desktop Best Practices - https://www.microsoft.com/en-us/security/business/security-101/how-to-secure-remote-desktop\n");
                        break;
                    case 5432:
                        linksText.append("1. PostgreSQL Basics - https://www.youtube.com/watch?v=qw--VYLpxG4\n")
                                .append("2. PostgreSQL Docs - https://www.postgresql.org/docs/\n")
                                .append("3. PostgreSQL Course - https://www.udemy.com/course/postgresql-mastery/\n")
                                .append("4. PostgreSQL Security - https://www.postgresql.org/docs/current/security.html\n")
                                .append("5. Database Security Guide - https://owasp.org/www-project-top-ten/\n");
                        break;
                    case 8080:
                        linksText.append("1. Proxy Basics - https://www.youtube.com/watch?v=5c-hCfqi1sM\n")
                                .append("2. HTTP Proxy Guide - https://www.cloudflare.com/learning/cdn/what-is-a-proxy-server/\n")
                                .append("3. Networking Course - https://www.linkedin.com/learning/networking-foundations-networking-basics\n")
                                .append("4. Proxy Security - https://owasp.org/www-community/attacks/Proxy_attacks\n")
                                .append("5. Web Server Security - https://www.nginx.com/resources/glossary/web-server-security/\n");
                        break;
                    case 8443:
                        linksText.append("1. HTTPS Explained - https://www.youtube.com/watch?v=T4Df5_cojAs\n")
                                .append("2. SSL/TLS Guide - https://www.cloudflare.com/learning/ssl/what-is-ssl/\n")
                                .append("3. Security Course - https://www.pluralsight.com/courses/ssl-tls-fundamentals\n")
                                .append("4. TLS Best Practices - https://www.ssllabs.com/projects/best-practices/\n")
                                .append("5. SSL/TLS Tutorial - https://www.digicert.com/what-is-ssl-tls-and-https\n");
                        break;
                    default:
                        linksText.append("1. Networking Basics - https://www.youtube.com/watch?v=qiQR5rTSshw\n")
                                .append("2. Port Reference - https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml\n")
                                .append("3. Networking Course - https://www.coursera.org/learn/computer-networking\n")
                                .append("4. Network Security - https://www.kaspersky.com/resource-center/definitions/what-is-network-security\n")
                                .append("5. Port Scanning Guide - https://nmap.org/book/man-port-scanning-techniques.html\n");
                }
            } catch (NumberFormatException e) {
                linksText.append("1. Networking Basics - https://www.youtube.com/watch?v=qiQR5rTSshw\n")
                        .append("2. General Guide - https://www.cisco.com/c/en/us/support/docs/ip/routing-information-protocol-rip/13788-3.html\n")
                        .append("3. Network Security - https://www.kaspersky.com/resource-center/definitions/what-is-network-security\n")
                        .append("4. Port Scanning Guide - https://nmap.org/book/man-port-scanning-techniques.html\n");
            }
        } else {
            linksText.append("1. IP Basics - https://www.youtube.com/watch?v=thhG-P0-88k\n")
                    .append("2. IP Addressing Guide - https://www.cisco.com/c/en/us/support/docs/ip/routing-information-protocol-rip/13788-3.html\n")
                    .append("3. Networking Course - https://www.udemy.com/course/complete-networking-fundamentals-course-ccna-start/\n")
                    .append("4. Subnetting Tutorial - https://www.youtube.com/watch?v=5WIFMz4AroE\n")
                    .append("5. IP Tools - https://www.whois.com/whois/\n")
                    .append("6. Network Security - https://www.cisa.gov/topics/cybersecurity-best-practices/network-security\n")
                    .append("7. IP Security Guide - https://www.paloaltonetworks.com/cyberpedia/what-is-ip-security-ipsec\n");
        }

        SpannableString spannableString = new SpannableString(linksText.toString());
        String[] lines = linksText.toString().split("\n");
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