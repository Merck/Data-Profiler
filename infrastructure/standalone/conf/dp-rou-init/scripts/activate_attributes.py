#!/usr/local/bin/python3

import requests
import psycopg
from string import Template

# POSTGRES CONFIG
PSQL_POD = 'dp-postgres'
DB_NAME = 'rules_of_use'
DB_USER = 'postgres'
DB_PASS = 'postgres'
APP_KEY = 'dp-rou-key'

# ROU CONFIG
ROU_HOSTNAME = 'dp-rou'

ATTRIBUTES_FILE = 'default_attributes'

USERS = ['developer', 'local-developer']

ROU_URL = f'http://{ROU_HOSTNAME}:8081/graphql'

ACTIVATE_ATTRIBUTE_QUERY = Template("""
mutation {
    markAttributeActive(
        value: "$attribute"
    ) {
        id
        value
        is_active
    }
}
""")

CREATE_USER_QUERY = Template("""
mutation {
    createUpdateUser(
        username: "$username", 
        attributes: $attributes
    ) {
        id
        username
        first_name
        last_name
        position
        attributes {
            id
            is_active
            value
        }        
    }
}
""")


ROU_HEADERS = {'Content-Type': 'application/json',
               'Authorization': 'dp-rou-key'}


def main():

    with psycopg.connect(host=PSQL_POD,
                         dbname=DB_NAME,
                         user=DB_USER,
                         password=DB_PASS) as conn:

        with conn.cursor() as cur:
            print('Adding application key to postgres')
            print(f'Setting application key to {APP_KEY}')
            cur.execute(f"""
            INSERT INTO applications (app,key,created_at,updated_at) VALUES ('Data Profiler', '{APP_KEY}', current_timestamp, current_timestamp)
            """)

            cur.execute("SELECT * FROM applications")
            print('Application Keys:')
            for record in cur.fetchall():
                print(record)

            conn.commit()

    attributes = []
    # Get attributes from the attribute file
    with open(ATTRIBUTES_FILE, 'r') as f:
        lines = f.readlines()
        for line in lines:
            attributes.append(line.strip())

    print('Adding attributes to ROU')
    for attribute in attributes:
        print(f'Adding attribute: {attribute}')
        query = ACTIVATE_ATTRIBUTE_QUERY.substitute(attribute=attribute)
        response = requests.post(
            url=ROU_URL, headers=ROU_HEADERS, json={'query': query})

        if response.status_code == 200:
            print(f'Success ({response.status_code}): {response.text}')
        else:
            print(f'Failure ({response.status_code}): {response.text}')

    # Convert list to string
    attributes_s = '[' + ','.join(f'"{x}"' for x in attributes) + ']'

    print('Adding users to the ROU')
    for user in USERS:
        print(f'Adding user: {user} with attributes: {attributes_s}')
        query = CREATE_USER_QUERY.substitute(
            username=user,
            attributes=attributes_s)
        response = requests.post(
            url=ROU_URL,
            headers=ROU_HEADERS,
            json={'query': query})

        if response.status_code == 200:
            print(f'Success ({response.status_code}): {response.text}')
        else:
            print(f'Failure ({response.status_code}): {response.text}')


if __name__ == '__main__':
    main()
