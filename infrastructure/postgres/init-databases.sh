#!/bin/bash
set -e

# ==============================================================================
# Multi-database initialization script for PostgreSQL container
# Automatically executed by /docker-entrypoint-initdb.d/ on first startup
# ==============================================================================

echo ">>> Initializing microservices databases for ai-business-platform..."

DATABASES=("auth_db" "customer_db" "product_db" "sales_db" "analytics_db")

for db in "${DATABASES[@]}"; do
    echo ">>> Ensuring database '$db' exists..."
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        SELECT 'CREATE DATABASE $db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
        GRANT ALL PRIVILEGES ON DATABASE $db TO $POSTGRES_USER;
EOSQL
done

echo ">>> Multi-database initialization complete."
