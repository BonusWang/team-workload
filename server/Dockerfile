FROM registry.minshenglife.com/infrastructure/java:8u372-fonts

RUN mkdir -p /apps
ADD ./target/*.jar /apps/app.jar

RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo "Asia/Shanghai" > /etc/timezone

CMD java -Duser.timezone=GMT+08 -Dfile.encoding=UTF-8 $JAVA_OPTS -jar /apps/app.jar
