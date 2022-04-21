FROM kingfalse/java8

MAINTAINER suranyi suranyi.sysu@gmail.com

LABEL create_time=2021.4.27

COPY ./gbc-1.1.jar /home/bin/

RUN echo "export GBC='/home/bin/gbc-1.1.jar'">>~/.bashrc \ 
    && echo "alias GBC='java -Xms4g -Xmx4g -jar /home/bin/gbc-1.1.jar'">>~/.bashrc

CMD java -version && bash