package main

import (
	"fmt"
	"log"

	"nginx-ldap-auth/data"
	"nginx-ldap-auth/group"
	"nginx-ldap-auth/ldap"
	"nginx-ldap-auth/rule"
	"nginx-ldap-auth/user"
)

func main() {
	file, config, err := parseConfig()
	if err != nil {
		log.Fatalln(err.Error())
	}

	fmt.Printf("Loaded config \"%s\".\n", file)

	pool := ldap.NewPool(config.Servers, config.Auth.BindDN, config.Auth.BindPW)

	err = pool.Connect()
	if err != nil {
		log.Fatalf("Error on connect to LDAP: %v\n", err)
	}

	storage := data.NewStorage(config.Timeout.Success, config.Timeout.Wrong)

	userService := user.NewService(pool, config.User.BaseDN, config.User.Filter)

	groupService := group.NewService(pool, config.Group.BaseDN, config.Group.Filter, config.Group.GroupAttr)

	ruleService := rule.NewService(storage, userService, groupService, config.User.RequiredGroups)

	fmt.Printf("Serving...\n")
	err = startServer(ruleService, config.Web, config.Path, config.Message)
	if err != nil {
		log.Fatalf("Error on start server: %v\n", err)
	}
}
