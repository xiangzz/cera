# Docker image for springboot application
# VERSION 0.0.1
# Author: zyh

# 构建镜像基于哪个镜像
FROM mayan31370/openjdk-alpine-with-chinese-timezone:8-jdk
# 镜像维护者姓名或邮箱地址
MAINTAINER zyh
# 为 RUN、CMD、ENTRYPOINT、COPY 和 ADD 设置工作目录，就是切换目录
WORKDIR /
#系统编码
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8
# 拷贝文件或目录到容器中，跟ADD类似，但不具备自动下载或解压的功能
COPY exec-jar-with-dependencies.jar exec-jar-with-dependencies.jar
#启动容器时的进程
#ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS $ENV_OPTS $REMOTE2 -jar ko-assy-db.jar" ]
CMD ["java", "-version"]
# 容器启动时执行的命令
ENV ID=""
#ENTRYPOINT ["java","-jar","/hello-jar-with-dependencies.jar", "$PORT"]
#ENTRYPOINT [ "sh", "-c", "java $PORT -jar /hello-jar-with-dependencies.jar" ]
#ENTRYPOINT ["/bin/bash"]
EXPOSE 8899
ENTRYPOINT java -jar /exec-jar-with-dependencies.jar $ID
