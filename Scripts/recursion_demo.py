import json


def process_child(child, depth=0):
    not_child = filter(lambda k: k != 'child', child.keys())
    for nc in not_child:
        print(f'depth = {depth}, {nc}: {child[nc]}')
    if 'child' in child.keys():
        process_child(child['child'], depth + 1)


if __name__ == '__main__':
    JSON = '''
    {
        "root": {
            "child": {
                "child": {
                    "child": {
                        "actual": "data",
                        "number":1
                    },
                    "sibling": "just data"
                },
                "sibling": "just data"
            }
        }
    }
'''

    data = json.loads(JSON)
    process_child(data['root'])
