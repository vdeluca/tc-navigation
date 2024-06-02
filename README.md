# Comando para instalar la app luego del clone

```
# Para descargar las dependencias
git submodule update --init --recursive

# Para setear la aplicación
./configure.sh

# Crear entorno para utilizar el map generator
# https://github.com/vdeluca/tc-navigation/tree/master/tools/python/maps_generator
source venv/bin/activate

# Para hacer un primer build 
./tools/unix/build_omim.sh -r generator_tool
cd tools/python/maps_generator

pip install -r requirements_dev.txt 

cp var/etc/map_generator.ini.default var/etc/map_generator.ini

python -m maps_generator --countries="Argentina_Pampas" --skip="Coastline"

```
