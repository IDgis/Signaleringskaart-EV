#!/bin/bash

searchString="expose_php = On"
replacestring="expose_php = Off"

find -name php.ini -type f -exec sed -i "s/$searchString/$replacestring/g" {} \;
