up:
	docker-compose up -d

down:
	docker-compose down

test-listener:
	cd listener && mvn test

run-listener:
	cd listener && mvn spring-boot:run
