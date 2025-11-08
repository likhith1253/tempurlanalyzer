# Use official Python runtime as a parent image
FROM python:3.9-slim

# Set the working directory in the container
WORKDIR /app

# Copy the current directory contents into the container at /app
COPY . .

# Install any needed packages specified in requirements.txt
# Note: mitmproxy is installed directly as it's the main dependency
RUN pip install --no-cache-dir mitmproxy==8.1.1 requests==2.31.0

# Make port 8081 available to the world outside this container
EXPOSE 8081

# Environment variables
ENV BACKEND_URL=http://backend:8080/analyze/url
ENV REQUEST_TIMEOUT=5
ENV CACHE_TTL=3600
ENV BLACKLIST_FILE=/data/blacklist.txt
ENV LOG_FILE=/data/sentinel_proxy.log
ENV DB_FILE=/data/sentinel_cache.db

# Create a volume for persistent data
VOLUME /data

# Make script executable
RUN chmod +x sentinel_proxy.py

# Run the proxy when the container launches
ENTRYPOINT ["mitmdump", "-s", "sentinel_proxy.py", "--listen-port", "8081", "--set", "confdir=/data"]
