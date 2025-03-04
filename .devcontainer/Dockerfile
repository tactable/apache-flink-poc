FROM mcr.microsoft.com/devcontainers/java:21-bullseye

# Create flink user and group
RUN groupadd --system --gid 9999 flink \
    && useradd --system --home-dir /home/flink --uid 9999 --gid flink flink \
    && mkdir -p /home/flink \
    && chown -R flink:flink /home/flink

