
My home IP (dynamic): 76.146.73.81


# EC2

EC2 instance `systemctl` commands:
- `sudo systemctl daemon-reload`
- `sudo systemctl restart tomcat`
- `systemctl status rabbitmq-server`

ssh into Amazon Linux EC2 instance command: ```
```
ssh -i CS6650.pem ec2-user@instance-address
```

ssh into Ubuntu EC2 instance command:
```
ssh -i CS6650.pem ubuntu@instance-address
```

IP for Ubuntu EC2 instance for "swipe-like-dislike-stats-consumer": `54.191.75.144`

IP for Ubuntu EC2 instance for "swipe-liked-users-stats-consumer": `44.231.192.218`

# Tomcat

Tomcat admin creds:
- username: admin
- password: admin666

Tomcat server EC2 instance elastic ip: `54.185.208.4`

location of tomcat's `server.xml` on EC2 instance:
```
/usr/share/apache-tomcat-9.0.63/conf/server.xml
```

ssh into Tomcat EC2 instance:
```
ssh -i CS6650.pem ec2-user@54.185.208.4
```
or:
```
ssh -i CS6650.pem ec2-user@35.160.135.31
```


scp into tomcat webapps dir (for placing war files):
```
scp -i CS6650.pem <path/to/war> ec2-user@instance-address:/usr/share/tomcat/webapps
```

How to automatically start Java jar files on AWS EC2:
1) Create unit file and save it in `/etc/systemd/system/my-app.service`
2) Run `sudo systemctl daemon-reload`
3) Run `sudo systemctl enable --now my-app`
4) Following commands become available
	```
	sudo systemctl status my-app
	sudo systemctl stop my-app
	sudo systemctl start my-app
	```
- References:
	- https://stackoverflow.com/questions/69678243/how-to-automatically-start-2-java-jars-on-aws-ec2
	- https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/8/html/configuring_basic_system_settings/assembly_working-with-systemd-unit-files_configuring-basic-system-settings



## Deploying a WAR file to Tomcat server in EC2 instance 

1) Open the **Build** menu and click **Build Artifacts** and click “Edit” in “Action”
2) In “Artifacts” tab of Project Settings, click “+” to add “Web Application: Archive” for “<WEB_APP>:war exploded” and click “OK” to finish. 
3) Open the **Build** menu and click **Build Artifacts** and click “Build” for “<WEB_APP>:war”
	- A WAR file called “<WEB_APP>.war” will be created in `out/artifacts/<WEB_APP>/<WEB_APP>.war`
4) Upload this file to the `webapps` folder in your Tomcat installation path in the EC2 instance with tools such as [scp](http://man7.org/linux/man-pages/man1/scp.1.html)
	- e.g. `scp -i CS6650.pem <path/to/war> ec2-user@instance-address:/usr/share/tomcat/webapps`
	- The exact location is dependent on installation choices and version but there’s a good chance it will be something like ‘/var/lib/tomcat8/webapps’ or alternatively /usr/share/tomcat8/webapps If you get permission denied, you’ll have to use chmod.
		- e.g. `sudo chmod -R 777 webapps/`
5) Visit **http://<YOUR_REMOTE_INSTANCE_IP>:8080/<WEB_APP>/<API_Gateway_Path>** and you should see the same response as what you get locally

## Tomcat Monitoring Tools

https://www.datadoghq.com/blog/tomcat-monitoring-tools/



# RabbitMQ

Management console can be accessed via browser by visiting: `<ipaddr>:15672`

creds:
- username: admin
- password: admin666

RMQ EC2 instance elastic ip: `54.68.180.97`


# Java

Compiling Java applications tutorial: https://www.jetbrains.com/help/idea/compiling-applications.html