# VPS network and Nginx

The Spring Boot container listens on port 8080 internally and is published only as `127.0.0.1:8080`. Nginx is the public HTTP entry point on port 80. PostgreSQL stays on its private Docker network.

Install the Nginx site file at `/etc/nginx/sites-available/campus-hub-events.conf` and enable it with a symlink in `/etc/nginx/sites-enabled/`. Remove only stale symlinks whose target no longer exists. Before changing the app port, run `sudo nginx -t`; after the container releases port 80, enable Nginx and verify `curl -I http://127.0.0.1/` and `/actuator/health/readiness` through the proxy.

Enable UFW only after allowing the existing SSH management port and HTTP:

```sh
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw enable
sudo ufw status verbose
```

Do not expose port 8080 publicly. It is bound to loopback for the local Nginx upstream. Keep the cloud NSG's SSH and HTTP rules aligned with this design.
