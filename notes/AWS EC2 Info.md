
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

scp into tomcat webapps dir (for placing war files):
```
scp -i CS6650.pem <path/to/war> ec2-user@instance-address:/usr/share/tomcat/webapps
```


# RabbitMQ

Management console can be accessed via browser by visiting: `<ipaddr>:15672`

creds:
- username: admin
- password: admin666

RMQ EC2 instance elastic ip: `54.68.180.97`


