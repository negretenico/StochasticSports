build:
	cd listener && mvn package -DskipTests

up:
	docker-compose up -d kafka kafka-init

down:
	docker-compose down

test-listener:
	cd listener && mvn test

run-listener:
	cd listener && mvn spring-boot:run

run-listener-dev:
	cd listener && mvn spring-boot:run -Dspring-boot.run.profiles=dev

logs:
	docker-compose logs -f
