#!/usr/bin/env bash
# One-time setup for a fresh Ubuntu/Debian VPS: Docker Engine + compose plugin, a firewall that only
# exposes SSH/HTTP/HTTPS, and a deploy user in the docker group. Run as root on the VPS:
#   curl -fsSL <raw url> | sudo bash -s -- deploy
set -euo pipefail

DEPLOY_USER="${1:-deploy}"

. /etc/os-release
apt-get update
apt-get install -y ca-certificates curl ufw
install -m 0755 -d /etc/apt/keyrings
curl -fsSL "https://download.docker.com/linux/$ID/gpg" -o /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/$ID $VERSION_CODENAME stable" \
  > /etc/apt/sources.list.d/docker.list
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

id "$DEPLOY_USER" >/dev/null 2>&1 || adduser --disabled-password --gecos "" "$DEPLOY_USER"
usermod -aG docker "$DEPLOY_USER"
install -d -m 700 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "/home/$DEPLOY_USER/.ssh"
echo "Add your public key to /home/$DEPLOY_USER/.ssh/authorized_keys"

ufw default deny incoming
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 443/udp
ufw --force enable
