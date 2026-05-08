#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTAINER_NAME="vcd-sqlserver"
SA_PASSWORD="VccdLocal123!"
JAVA_HOME_CANDIDATE="/opt/homebrew/opt/openjdk@17"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令: $1"
    exit 1
  fi
}

wait_for_docker() {
  local retries=30
  for _ in $(seq 1 "$retries"); do
    if docker ps >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "Docker 运行时未就绪。"
  exit 1
}

ensure_colima() {
  require_command colima
  require_command docker

  if ! colima status >/dev/null 2>&1; then
    echo "正在启动 Colima..."
    colima start >/dev/null
  fi

  wait_for_docker
}

ensure_sql_container() {
  local status
  status="$(docker ps -a --filter "name=^/${CONTAINER_NAME}$" --format '{{.Status}}')"

  if [ -z "$status" ]; then
    echo "正在创建 SQL Server 容器..."
    docker run -d \
      --name "$CONTAINER_NAME" \
      -e ACCEPT_EULA=Y \
      -e MSSQL_SA_PASSWORD="$SA_PASSWORD" \
      -p 1433:1433 \
      mcr.microsoft.com/azure-sql-edge:latest >/dev/null
  elif ! docker ps --filter "name=^/${CONTAINER_NAME}$" --format '{{.Names}}' | rg -x "$CONTAINER_NAME" >/dev/null 2>&1; then
    echo "正在启动 SQL Server 容器..."
    docker start "$CONTAINER_NAME" >/dev/null
  fi
}

wait_for_sql() {
  local retries=60

  for _ in $(seq 1 "$retries"); do
    if docker exec "$CONTAINER_NAME" /bin/sh -lc "printf '[mssql]\nDriver=ODBC Driver 17 for SQL Server\nServer=localhost\nDatabase=master\n' > /tmp/odbc.ini && printf 'SELECT 1;\n' | ODBCINI=/tmp/odbc.ini isql -v mssql sa '$SA_PASSWORD'" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done

  echo "SQL Server 容器已启动，但数据库未在预期时间内就绪。"
  exit 1
}

ensure_database() {
  echo "正在确认数据库 vcddb..."
  docker exec "$CONTAINER_NAME" /bin/sh -lc "printf '[mssql]\nDriver=ODBC Driver 17 for SQL Server\nServer=localhost\nDatabase=master\n' > /tmp/odbc.ini && printf \"IF DB_ID('vcddb') IS NULL CREATE DATABASE vcddb;\n\" | ODBCINI=/tmp/odbc.ini isql -v mssql sa '$SA_PASSWORD'" >/dev/null
}

ensure_java() {
  if [ -d "$JAVA_HOME_CANDIDATE" ]; then
    export JAVA_HOME="$JAVA_HOME_CANDIDATE"
    export PATH="$JAVA_HOME/bin:$PATH"
    return 0
  fi

  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    local resolved_java_home
    if resolved_java_home="$("/usr/libexec/java_home" -v 17 2>/dev/null)"; then
      export JAVA_HOME="$resolved_java_home"
      export PATH="$JAVA_HOME/bin:$PATH"
      return 0
    fi
  fi

  if command -v java >/dev/null 2>&1; then
    return 0
  fi

  echo "未找到可用的 JDK 17。"
  exit 1
}

main() {
  require_command rg

  ensure_colima
  ensure_sql_container
  echo "SQL Server 就绪检测..."
  wait_for_sql
  ensure_database
  ensure_java

  echo "正在启动后端..."
  cd "$ROOT_DIR"
  ./maven_bin/bin/mvn -f vcd-backend/pom.xml spring-boot:run
}

main "$@"
