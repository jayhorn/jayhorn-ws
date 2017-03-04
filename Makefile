
all:
	./gradlew war
	docker build -t webhorn .
	docker run -it --rm -p 8888:8080 webhorn