web.xml : -definir le servlet FrontController de controllers.FrontController
          -definir le context-param : - controller_package (name)
                                      - nom de votre package de controller (value)
          -servlet mapping : - FrontController
                             - url : /

annotation : - Controller : tous les class qui jouent le role de controller
             - Get : les methodes de controller appeller par lien