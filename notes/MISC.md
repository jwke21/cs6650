
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


# Tomcat

Tomcat admin creds:
- username: admin
- password: admin666

Tomcat server EC2 instance elastic ip: `54.185.208.4`

location of tomcat's `server.xml` on EC2 instance:
```
/usr/share/apache-tomcat-9.0.63/conf/server.xml
```

ssh into tomcat EC2 instance:
```
ssh -i CS6650.pem ec2-user@54.185.208.4
```

scp into tomcat webapps dir (for placing war files):
```
scp -i CS6650.pem <path/to/war> ec2-user@instance-address:/usr/share/tomcat/webapps
```

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


