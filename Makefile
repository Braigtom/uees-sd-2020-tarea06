.PHONY: clean colector servidor all

default: all

all: colector servidor

colector: colector.jar

colector.jar: manifest_colector.mf build/sensores/Colector.class build/sensores/Lectura.class
	jar cmf manifest_colector.mf colector.jar -C build sensores/Colector.class -C build sensores/Lectura.class
	rm -rf manifest_colector.mf

manifest_colector.mf:
	echo "Main-Class: sensores.Colector" > manifest_colector.mf

build/sensores/Colector.class: src/sensores/Colector.java
	mkdir -p build
	javac -cp src/ src/sensores/Colector.java -d build

build/sensores/Lectura.class: src/sensores/Lectura.java
	mkdir -p build
	javac -cp src/ src/sensores/Lectura.java -d build


servidor: servidor.jar

servidor.jar: manifest_servidor.mf build/sensores/Servidor.class build/sensores/Lectura.class
	jar cmf manifest_servidor.mf servidor.jar -C build sensores/Servidor.class -C build sensores/Lectura.class
	rm -rf manifest_servidor.mf

manifest_servidor.mf:
	echo "Main-Class: sensores.Servidor" > manifest_servidor.mf

build/sensores/Servidor.class: src/sensores/Servidor.java
	mkdir -p build
	javac -cp src/ src/sensores/Servidor.java -d build


clean:
	rm -rf build *.jar


