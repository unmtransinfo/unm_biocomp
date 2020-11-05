#
FROM ubuntu:20.04
WORKDIR /home/app
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update
RUN apt-get install -y sudo
RUN apt-get install -y zip
RUN apt-get install -y inetutils-ping
RUN apt-get install -y apt-utils
ENV TZ=America/Denver
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ >/etc/timezone
RUN apt-get install -y tzdata
RUN apt-get install -y curl
RUN echo "=== Done installing Ubuntu."
#
###
RUN apt-get update
RUN apt-get install -y openjdk-8-jdk
RUN apt-cache policy openjdk-8-jdk
RUN java -version
RUN echo "=== Done installing Java."
#
###
RUN apt-get install -y software-properties-common
RUN apt-add-repository -y "deb http://us.archive.ubuntu.com/ubuntu/ bionic universe"
RUN apt-get install -y tomcat9 tomcat9-admin
RUN apt-cache policy tomcat9
RUN cp -r /etc/tomcat9 /usr/share/tomcat9/conf
COPY conf/tomcat/tomcat-users.xml /usr/share/tomcat9/conf/
COPY conf/tomcat/server.xml /usr/share/tomcat9/conf/
RUN mkdir /usr/share/tomcat9/temp
RUN mkdir /usr/share/tomcat9/logs
RUN echo "=== Done installing Tomcat."
#
###
# WAR should auto-deploy, but seems we must manually unzip.
COPY biocomp_war/target/biocomp_war-0.0.1-SNAPSHOT.war /usr/share/tomcat9/webapps/biocomp.war
#RUN i=0; while [ ! -d /usr/share/tomcat9/webapps/biocomp ]; do sleep 1; i=$(($i+1)); printf "%d. Waiting for auto-deploy...\n" "$i"; done
RUN cd /usr/share/tomcat9/webapps; mkdir biocomp; cd biocomp; unzip ../biocomp.war
RUN chown -R tomcat /usr/share/tomcat9/webapps/biocomp
RUN ls -laR /usr/share/tomcat9/webapps
RUN echo "=== Done installing application BIOCOMP."
#
###
# web.xml has db access credentials.
RUN mkdir -p /home/app/conf/biocomp
COPY conf/biocomp/web.xml /home/app/conf/biocomp
RUN cp /home/app/conf/biocomp/web.xml /usr/share/tomcat9/webapps/biocomp/WEB-INF
RUN chown tomcat /usr/share/tomcat9/webapps/biocomp/WEB-INF/web.xml
RUN echo "=== Done configuring application BIOCOMP."
#
###
# Needed? Tomcat run by root, apparently.
RUN /usr/share/tomcat9/bin/catalina.sh start
RUN echo "=== Done starting Tomcat."
#
###
# psql useful for testing.
RUN apt-get install -y postgresql-client-12
COPY conf/postgresql/.pgpass /home/app
RUN chmod 600 /home/app/.pgpass
RUN echo "=== Done installing Postgresql (client)."
#
###
CMD ["/usr/share/tomcat9/bin/catalina.sh", "run"]
#
