'''
GitHub organization administration utility.

Run ``python3 gh_org_admin.py -h`` for how to use.
'''


from argparse import ArgumentParser
from argparse import FileType
from argparse import RawDescriptionHelpFormatter
import json
import logging
import os
import sys
import urllib.request


__VERSION__ = '1.0'

DESC = 'Administer a GitHub organization'
GH_ACCEPT = 'application/vnd.github.v3+json'
GH_ACCEPT_PREVIEW = 'application/vnd.github.baptiste-preview+json'
GH_CONTENT_TYPE = 'application/json'
GH_PAT = 'GITHUB_PERSONAL_ACCESS_TOKEN'


def var_state(**kwargs):
    ''' Returns a list of variable names and values. 

        Sample usage::

           >>> var_state(foo=1, bar='bar')
           ['foo: 1', 'bar: bar']
    '''
    return [f'{k}: {v}' for k, v in kwargs.items()]


def naive_slugify(text):
    ''' Returns the slug of the given text.

        The slug is generated in a very naive way where all 
        characters are converted to lower case and spaces are
        replaced with '-' characters.

        :param text: the text to turn into a slug.
        :return: slug of text
        :rtype: str
    '''
    return text.replace(' ', '-').lower()


def add_org_user(org=None, data={}):
    ''' And a user to the GitHub organization. Returns dict of 
        invitation status.

        See `GitHub API documentation <https://docs.github.com/en/rest/reference/orgs#create-an-organization-invitation>`_  
        for invitation data.

        :param org: GitHub organization
        :param data: invitation specification
        :return: invitation status.
        :rtype: dict
    '''
    data = json.dumps(data, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/orgs/{org}/invitations',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Content-Type': f'{GH_CONTENT_TYPE}',
            'Accept': f'{GH_ACCEPT}'},
        data=data,
        method='POST')

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def add_org_team_member(org=None, team_slug=None, user=None, data={}):
    ''' Add an org member to a team. Returns dict of status of add.

        See `GitHub API documentation <https://docs.github.com/en/rest/reference/teams#add-or-update-team-membership-for-a-user>`_  
        for team member data.

        :param org: the GitHub organization name
        :param team_slug: team slug
        :param user: the GitHub user name
        :param data: repository specifications dict
        :return: created repository metadata
        :rtype: dict
    '''
    data = json.dumps(data, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/orgs/{org}/teams/{team_slug}/memberships/{user}',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Content-Type': f'{GH_CONTENT_TYPE}',
            'Accept': f'{GH_ACCEPT}'},
        data=data,
        method='PUT')

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def create_org_team(org=None, data={}):
    ''' Create an organizational team. Returns the created
        team metadata from GitHub.

        See `GitHub API documentation <https://docs.github.com/en/rest/reference/teams#create-a-team>`_
        for team specification data.

        :param org: the GitHub organization name.
        :param data: team specifications dict.
        :return: created team metadata.
        :rtype: dict
    '''
    data = json.dumps(data, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/orgs/{org}/teams',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Content-Type': f'{GH_CONTENT_TYPE}',
            'Accept': f'{GH_ACCEPT}'},
        data=data,
        method='POST')

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def create_repository(org=None, data={}):
    ''' Create an organizational repository. Returns the created
        repository metadata from GitHub.

        See `GitHub API documentation <https://docs.github.com/en/rest/reference/repos#create-an-organization-repository>`_
        for repository specification data.

        :param org: the GitHub organization name.
        :param data: repository specifications dict.
        :return: created repository metadata.
        :rtype: dict
    '''
    data = json.dumps(data, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/orgs/{org}/repos',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Content-Type': f'{GH_CONTENT_TYPE}',
            'Accept': f'{GH_ACCEPT}'},
        data=data,
        method='POST')

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def create_repository_from_template(org=None, template_repo=None, data={}):
    ''' Create a repository from a template repository. Returns the
        created repository metadata from GitHub.

        See GitHub API documentation `<https://docs.github.com/en/rest/reference/repos#create-a-repository-using-a-template>`_
        for repository specification data.

        :param org: the GitHub organization name.
        :param template_repo: the GitHub template repository name.
        :param data: repository specifications dict.
        :return: created repository metadata.
        :rtype: dict
    '''
    data = json.dumps(data, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{org}/{template_repo}/generate',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Content-Type': f'{GH_CONTENT_TYPE}',
            'Accept': f'{GH_ACCEPT_PREVIEW}'},
        data=data,
        method='POST')

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def get_user(user=None):
    ''' Returns user information.

        :param user: GitHub user name
        :return: dict of user details
        :rtype: dict
    '''
    req = urllib.request.Request(
        url=f'https://api.github.com/users/{user}',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Accept': f'{GH_ACCEPT}'},
        method='GET')

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def rm_org_team(org=None, team_slug=None):
    ''' Removes the given team from the organization.

        :param org: GitHub organization
        :param team_slug: team slug
    '''
    req = urllib.request.Request(
        url=f'https://api.github.com/orgs/{org}/teams/{team_slug}',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Accept': f'{GH_ACCEPT}'},
        method='DELETE')

    with urllib.request.urlopen(req) as resp:
        pass


def rm_org_team_member(org=None, team_slug=None, user=None):
    ''' Remove an org member from a GitHub team.

        See `GitHub API documentation <https://docs.github.com/en/rest/reference/teams#remove-team-membership-for-a-user>`_ 
        for more information.

        :param org: GitHub organization
        :param team_slug: team slug
        :param user: the GitHub user name
    '''
    req = urllib.request.Request(
        url=f'https://api.github.com/orgs/{org}/teams/{team_slug}/memberships/{user}',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Accept': f'{GH_ACCEPT}'},
        method='DELETE')

    with urllib.request.urlopen(req) as resp:
        pass


def rm_org_user(org=None, user=None):
    ''' Removes a user from a GitHub organization.

        See `GitHub API documentation <https://docs.github.com/en/rest/reference/orgs#remove-an-organization-member>`_
        for more information.

        :param org: GitHub organization
        :param user: GitHub username
    '''
    req = urllib.request.Request(
        url=f'https://api.github.com/orgs/{org}/members/{user}',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Accept': f'{GH_ACCEPT}'},
        method='DELETE')

    with urllib.request.urlopen(req) as resp:
        pass


def rm_team_permission(org=None, team_slug=None, owner=None, repo=None):
    ''' Removes the given team's permission from the repository.

        :param org: GitHub organization
        :param team_slug: team slug
        :param owner: repository owner
        :param repo: repository name
    '''
    req = urllib.request.Request(
        url=f'https://api.github.com/orgs/{org}/teams/{team_slug}/repos/{owner}/{repo}',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Accept': f'{GH_ACCEPT}'},
        method='DELETE')

    with urllib.request.urlopen(req) as resp:
        pass


def rm_user_permission(owner=None, user=None, repo=None):
    ''' Removes the given user from the given repository.

        :param owner: repository owner
        :param user: GitHub username
        :param repo: repository name
        :return: response data if any
        :rtype: dict
    '''
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{owner}/{repo}/collaborators/{user}',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Accept': f'{GH_ACCEPT}'},
        method='DELETE')

    with urllib.request.urlopen(req) as resp:
        pass


def set_team_permission(org=None, team_slug=None, owner=None, repo=None, data={}):
    ''' Sets the permission for the given team on the given repository.

        :param org: GitHub organization
        :param team_slug: team slug
        :param owner: repository owner
        :param repo: repository name
        :param data: permission data dict
    '''
    data = json.dumps(data, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/orgs/{org}/teams/{team_slug}/repos/{owner}/{repo}',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Content-Type': f'{GH_CONTENT_TYPE}',
            'Accept': f'{GH_ACCEPT}'},
        data=data,
        method='PUT')

    with urllib.request.urlopen(req) as resp:
        pass


def set_user_permission(owner=None, user=None, repo=None, data={}):
    ''' Sets the permission for the given team on the given repository.

        :param owner: repository owner
        :param user: GitHub username
        :param repo: repository name
        :param data: permission data dict
    '''
    data = json.dumps(data, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{owner}/{repo}/collaborators/{user}',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Content-Type': f'{GH_CONTENT_TYPE}',
            'Accept': f'{GH_ACCEPT}'},
        data=data,
        method='PUT')

    with urllib.request.urlopen(req) as resp:
        resp_data = resp.read()
        if resp_data:
            return json.loads(resp_data)
        else:
            return {}


if __name__ == '__main__':

    NAME_KEYS = ('gh_repo', 'gh_team')
    LIST_KEYS = ('maintainers', 'repo_names', 'team_ids')
    EXCLUDED_KEYS = ('debug', 'func', 'generate_json_only', 'template_repo')
    FIRST_ITEM_KEYS = ('email', 'invitee_id', 'parent_team_id')

    def create_create_metadata_specs(**kwargs):
        ''' Create and return dict with create metadata specifications for 
            creating a GitHub repository or team.

            See `Create an organization repository <https://docs.github.com/en/rest/reference/repos#create-an-organization-repository>`_ 
            for values of the keyword arguments to create a repository.

            See `Create a team <https://docs.github.com/en/rest/reference/teams#create-a-team>`_ 
            for values of the keyword arguments to create a team.
        '''
        name = {'name': v[0] for k, v in kwargs.items() if k in NAME_KEYS}
        strs_and_bools = {k: v for k, v in kwargs.items()
                          if (isinstance(v, str) or isinstance(v, bool))
                          and k not in EXCLUDED_KEYS}
        lists = {k: v for k, v in kwargs.items() if v and k in LIST_KEYS}
        first_items_in_lists = {k: v[0] for k, v in kwargs.items()
                                if v and (k in FIRST_ITEM_KEYS)}
        return {**name, **strs_and_bools, **lists, **first_items_in_lists}

    def do_json_print(data={}, file=sys.stdout):
        ''' Convert data into json and print it.

            :param data: dict of data (empty by default)
            :param file: where to print to (sys.stdout by default)
        '''
        print(json.dumps(data, indent=2, sort_keys=True), file=file)

    def do_add_org_team_member(args):
        ''' Command-line exclusive add org member to team. Executes
            the add_org_team_member function with properly 
            constructed arguments.

            :param args: Namespace containing the parsed 
                            command line arguments.
            :return: response from GitHub.
            :rtype: dict
        '''
        data = {'role': args.gh_role}
        if args.generate_json_only:
            do_json_print(data=data)
            return {}

        return add_org_team_member(org=args.gh_org[0],
                                   team_slug=naive_slugify(args.gh_team[0]),
                                   user=args.user, data=data)

    def do_create_repository(args):
        ''' Command-line exclusive create repository. Executes
            the create_repository function with properly 
            constructed arguments.

            :param args: Namespace containing the parsed 
                            command line arguments.
            :return: response from GitHub.
            :rtype: dict
        '''
        data = create_create_metadata_specs(**vars(args))
        if args.generate_json_only:
            do_json_print(data=data)
            return {}
        return create_repository(org=args.gh_org[0], data=data)

    def do_create_repository_from_template(args):
        ''' Command-line exclusive create repository. Executes
            the create_repository_from_template function with
            properly constructed arguments.

            :param args: Namespace containing the parsed 
                            command line arguments.
            :return: response from GitHub.
            :rtype: dict
        '''
        if not args.owner:
            args.owner = args.gh_org[0]
        data = create_create_metadata_specs(**vars(args))
        org = args.gh_org[0]
        template_repo = args.template_repo[0]

        if args.generate_json_only:
            do_json_print(data=data)
            return {}
        return create_repository_from_template(org=org,
                                               template_repo=template_repo,
                                               data=data)

    def do_create_team(args):
        ''' Command-line exclusive create repository. Executes
            the create_org_team function with properly 
            constructed arguments.

            :param args: Namespace containing the parsed 
                            command line arguments.
            :return: response from GitHub.
            :rtype: dict
        '''
        data = create_create_metadata_specs(**vars(args))
        if args.generate_json_only:
            do_json_print(data=data)
            return {}
        return create_org_team(org=args.gh_org[0], data=data)

    def do_get_user(args):
        ''' Command-line exclusive get user. Executes
            the get_user function with properly constructed
            arguments.

            :param args: Namespace containing the parsed 
                            command line arguments.
            :return: response from GitHub.
            :rtype: dict
        '''
        return get_user(args.user)

    def do_invite_org_user(args):
        ''' Command-line exclusive get user. Executes
            the add_org_user function with properly
            constructed arguments.

            :param args: Namespace containing the parsed 
                            command line arguments.
            :return: response from GitHub.
            :rtype: dict
        '''
        data = create_create_metadata_specs(**vars(args))
        if args.generate_json_only:
            do_json_print(data=data)
            return {}
        return add_org_user(org=args.gh_org[0], data=data)

    def do_rm_org_team(args):
        ''' Command-line exclusive remove org team. Executes
            the different rm org_team functions with 
            properly constructed arguments.

            :param args: Namespace containing the parsed 
                            command line arguments.
        '''
        org = args.gh_org[0]
        return rm_org_team(org=org, team_slug=naive_slugify(args.gh_team[0]))

    def do_rm_org_team_member(args):
        ''' Command-line exclusive remove org team member. Executes
            the different rm org_team functions with 
            properly constructed arguments.

            :param args: Namespace containing the parsed 
                            command line arguments.
        '''
        rm_org_team_member(org=args.gh_org[0], team_slug=naive_slugify(
            args.gh_team[0]), user=args.user)
        return {}

    def do_rm_org_user(args):
        ''' Command-line exclusive remove a user from an org.
            Executes the rm_org_user function with properly
            constructed arguments.

            :param args: Namespace containing the parsed 
                            command line arguments.
        '''
        print('do_rm_org_user')

    def do_rm_permission(args):
        ''' Command-line exclusive remove permission. Executes
            the different rm permission functions with 
            properly constructed arguments.

            :param args: Namespace containing the parsed 
                            command line arguments.
        '''
        org = args.gh_org[0]
        try:
            owner = args.gh_owner[0]
        except:
            owner = org
        repo = args.gh_repo[0]
        if args.gh_team_or_user[0] == 'team':
            return rm_team_permission(org=org, team_slug=naive_slugify(args.gh_name[0]),
                                      owner=owner, repo=repo)
        else:
            return rm_user_permission(owner=owner, user=args.gh_name[0], repo=repo)

    def do_set_permission(args):
        ''' Command-line exclusive set permission. Executes
            the different set permission functions with 
            properly constructed arguments.

            :param args: Namespace containing the parsed 
                            command line arguments.
        '''
        org = args.gh_org[0]
        try:
            owner = args.gh_owner[0]
        except:
            owner = org
        repo = args.gh_repo[0]
        data = {'permission': args.gh_perm[0]}
        if args.generate_json_only:
            do_json_print(data=data)
            return {}
        if args.gh_team_or_user[0] == 'team':
            return set_team_permission(org=org,
                                       team_slug=naive_slugify(
                                           args.gh_name[0]),
                                       owner=owner, repo=repo, data=data)
        else:
            return set_user_permission(
                owner=owner, user=args.gh_name[0], repo=repo, data=data)

    if not os.getenv(GH_PAT):
        print(f'Set environment variable {GH_PAT}', file=sys.stderr)
        sys.exit(1)

    debug_group = ArgumentParser(
        formatter_class=RawDescriptionHelpFormatter, add_help=False)
    debug_group.add_argument('-d', '--debug', action='store_true',
                             help='Turn on debug logging')

    common_group = ArgumentParser(
        formatter_class=RawDescriptionHelpFormatter, add_help=False,
        parents=[debug_group])
    common_group.add_argument('gh_org', metavar='ORG', nargs=1,
                              help='GitHub organization')

    gh_json_group = ArgumentParser(
        formatter_class=RawDescriptionHelpFormatter, add_help=False)
    gh_json_group.add_argument('-j', '--generate-json-only', action='store_true',
                               help='Generate json, but do not send to GitHub')

    parser = ArgumentParser(description=DESC,
                            formatter_class=RawDescriptionHelpFormatter)

    subparsers = parser.add_subparsers(metavar='Entity',
                                       description='GitHub entities to administer',
                                       help='Description')

    repo_parser = subparsers.add_parser('repo', help='Administer repositories')

    repo_subparsers = repo_parser.add_subparsers(
        description='GitHub repository administration commands')

    create_repo_parser = repo_subparsers.add_parser('create', help='Create a new repository',
                                                    parents=[common_group, gh_json_group])
    create_repo_parser.add_argument('gh_repo', nargs=1, metavar='REPO',
                                    help='Name of the new GitHub repository')
    create_repo_parser.add_argument('-A', '--auto-init', action='store_true',
                                    help='Override default auto_init setting (False).')
    create_repo_parser.add_argument('-B', '--allow-rebase-merge', action='store_false',
                                    help='Override default allow_rebase_merge setting (True).')
    create_repo_parser.add_argument('-C', '--license-template', metavar='LICENSE_KEYWORD',
                                    help='Open source license to apply')
    create_repo_parser.add_argument('-D', '--description',
                                    help='A short description of the repository')
    create_repo_parser.add_argument('-E', '--is-template', action='store_true',
                                    help='Override default is_template setting (False).')
    create_repo_parser.add_argument('-G', '--gitignore-template', metavar='LANG_OR_PLATFORM',
                                    help='Desired language or platform .gitignore template to apply')
    create_repo_parser.add_argument('-H', '--homepage', metavar='URL',
                                    help='A URL with more information about the repository')
    create_repo_parser.add_argument('-I', '--has-issues', action='store_false',
                                    help='Override default has_issues setting (True).')
    create_repo_parser.add_argument('-L', '--delete-branch-on-merge', action='store_false',
                                    help='Override default delete_branch_on_merge setting (True).')
    create_repo_parser.add_argument('-M', '--allow-merge-commit', action='store_false',
                                    help='Override default allow_merge_commit setting (True).')
    create_repo_parser.add_argument('-P', '--private', action='store_false',
                                    help='Override default private repository setting (True).')
    create_repo_parser.add_argument('-R', '--has-projects', action='store_false',
                                    help='Override default has_projects setting (True).')
    create_repo_parser.add_argument('-S', '--allow-squash-merge', action='store_false',
                                    help='Override default allow_squash_merge setting (True).')
    # Disabled since the nebula-preview header has to be set, see
    # https://docs.github.com/rest/reference/repos#create-an-organization-repository
    # create_repo_parser.add_argument('-V', '--visibility', choices=[
    #                     'private', 'internal', 'public'], default='private',
    #                     help='Repository visibility (default is private)')
    create_repo_parser.add_argument('-W', '--has-wiki', action='store_false',
                                    help='Override default has_wiki setting (True).')
    create_repo_parser.set_defaults(func=do_create_repository)

    template_create_repo_parser = repo_subparsers.add_parser('template-create',
                                                             help='Create a new repository from template',
                                                             parents=[common_group, gh_json_group])
    template_create_repo_parser.add_argument('gh_repo', nargs=1, metavar='REPO',
                                             help='Name of the new GitHub repository')
    template_create_repo_parser.add_argument('template_repo', nargs=1, metavar='TEMPLATE-REPO',
                                             help='Name of template repository to use')
    template_create_repo_parser.add_argument('owner', nargs='?', metavar='OWNER',
                                             help='Optional owner of REPO (assumed to be ORG)')
    template_create_repo_parser.add_argument('-D', '--description',
                                             help='A short description of the repository')
    template_create_repo_parser.add_argument('-I', '--include-all-branches', action='store_true',
                                             help='Override default include_all_branches setting (False).')
    template_create_repo_parser.add_argument('-P', '--private', action='store_false',
                                             help='Override default private repository setting (True).')
    template_create_repo_parser.set_defaults(
        func=do_create_repository_from_template)

    permissions_group = ArgumentParser(
        formatter_class=RawDescriptionHelpFormatter, add_help=False)
    permissions_group.add_argument('gh_repo', nargs=1,
                                   metavar='REPO',
                                   help='Name of the GitHub repository')
    permissions_group.add_argument('gh_team_or_user', nargs=1,
                                   metavar='TEAM-OR-USER',
                                   choices=['team', 'user'],
                                   default='team',
                                   help='Permission for a team or user')
    permissions_group.add_argument(
        'gh_name', nargs=1, metavar='TEAM-OR-USER-NAME', help='Name of the team or user')

    perm_parser = subparsers.add_parser(
        'perm', help='Administer permissions (user or team)')

    perm_subparsers = perm_parser.add_subparsers(
        description='GitHub permission administration commands')

    rm_perm_parser = perm_subparsers.add_parser('revoke',
                                                help='Revoke a permission',
                                                parents=[common_group, permissions_group])

    rm_perm_parser.add_argument('gh_owner', nargs='?', metavar='OWNER',
                                help='Optional owner of REPO (assumed to be ORG)')
    rm_perm_parser.set_defaults(func=do_rm_permission)

    set_perm_parser = perm_subparsers.add_parser('set',
                                                 help='Set a permission',
                                                 parents=[common_group,
                                                          gh_json_group,
                                                          permissions_group])
    set_perm_parser.add_argument('gh_perm', nargs=1, metavar='PERMISSION',
                                 choices=['pull', 'push', 'admin',
                                          'maintain', 'triage'],
                                 default='pull',
                                 help='Permssion to grant')
    set_perm_parser.add_argument('gh_owner', nargs='?', metavar='OWNER',
                                 help='Optional owner of REPO (assumed to be ORG)')
    set_perm_parser.set_defaults(func=do_set_permission)

    team_group = ArgumentParser(
        formatter_class=RawDescriptionHelpFormatter, add_help=False)
    team_group.add_argument('gh_team', nargs=1, metavar='TEAM',
                            help='Name of GitHub team')

    user_group = ArgumentParser(
        formatter_class=RawDescriptionHelpFormatter, add_help=False)
    user_group.add_argument('user', metavar='USER', help='GitHub username')

    team_parser = subparsers.add_parser('team', help='Administer teams')

    team_subparsers = team_parser.add_subparsers(
        description='GitHub team administration commands')

    add_org_team_member_parser = team_subparsers.add_parser('add-org-member',
                                                            help='Adds a member of the organization to the team',
                                                            parents=[common_group,
                                                                     gh_json_group,
                                                                     team_group,
                                                                     user_group])
    add_org_team_member_parser.add_argument('gh_role', metavar='ROLE', nargs='?',
                                            choices=['member', 'maintainer'],
                                            default='member',
                                            help='Role for the user on the team (default is member)')
    add_org_team_member_parser.set_defaults(func=do_add_org_team_member)

    create_team_parser = team_subparsers.add_parser('create',
                                                    help='Create a new team',
                                                    parents=[common_group,
                                                             gh_json_group,
                                                             team_group])
    create_team_parser.add_argument('-D', '--description',
                                    help='A short description of the team')
    create_team_parser.add_argument('-I', '--parent-team-id', type=int, nargs=1,
                                    help='The ID of a team to set as the parent team')
    create_team_parser.add_argument('-M', '--maintainer', action='append', dest='maintainers',
                                    help='GitHub user name for org member that will be a maintainer of this group (can specify more than once)')
    create_team_parser.add_argument('-R', '--repo-name', action='append', dest='repo_names',
                                    help='Full repo name (org/repo-name) to add the team to (can specify more than once)')
    create_team_parser.add_argument('-P', '--privacy', nargs=1, metavar='PRIV_LEVEL',
                                    choices=['secret', 'closed'],
                                    default='secret',
                                    help='Level of privacy this team should have')
    create_team_parser.set_defaults(func=do_create_team)

    delete_team_parser = team_subparsers.add_parser('delete',
                                                    help='Delete a team',
                                                    parents=[common_group, team_group])
    delete_team_parser.set_defaults(func=do_rm_org_team)

    rm_org_team_member_parser = team_subparsers.add_parser('remove-org-member',
                                                           help='Remove an org member from a team.',
                                                           parents=[common_group, team_group, user_group])
    rm_org_team_member_parser.set_defaults(func=do_rm_org_team_member)

    user_parser = subparsers.add_parser('user', help='Run user commands')

    user_subparsers = user_parser.add_subparsers(
        description='GitHub user admin commands')
    id_user_parser = user_subparsers.add_parser('id',
                                                help='List user details',
                                                parents=[debug_group, user_group])
    id_user_parser.set_defaults(func=do_get_user)

    invite_user_parser = user_subparsers.add_parser('invite',
                                                    help='Invite a user to join an organization',
                                                    parents=[common_group, gh_json_group])
    invite_user_parser_email_or_id = invite_user_parser.add_mutually_exclusive_group(
        required=True)
    invite_user_parser_email_or_id.add_argument('-E', '--email', nargs=1, metavar='EMAIL',
                                                help='Email address of invitee')
    invite_user_parser_email_or_id.add_argument('-I', '--user-id', nargs=1, metavar='ID', type=int, dest='invitee_id',
                                                help='GitHub user ID of invitee')

    invite_user_parser.add_argument('-T', '--team-id', type=int, action='append', dest='team_ids',
                                    help='Team IDs to invite users to (can specify more than once)')
    invite_user_parser.add_argument('role', metavar='ROLE', nargs='?',
                                    help='Role for the user in the org (default is direct_member)',
                                    choices=['direct_member',
                                             'admin', 'billing_manager'],
                                    default='direct_member')
    invite_user_parser.set_defaults(func=do_invite_org_user)

    remove_user_parser = user_subparsers.add_parser('remove',
                                                    help='Remove a user from an organization',
                                                    parents=[common_group, user_group])

    args = parser.parse_args()

    try:
        if args.debug:
            logging.basicConfig(format='%(levelname)s: %(message)s',
                                level=logging.DEBUG)
    except AttributeError:
        parser.print_usage()
        sys.exit(1)

    err = None
    try:
        logging.debug(var_state(**vars(args)))
        resp = args.func(args)
        if resp:
            logging.debug(json.dumps(resp, sort_keys=True, indent=2))
    except AttributeError as e:
        print('Need to specify a command', file=sys.stderr)
        err = e
    except urllib.error.HTTPError as e:
        print(f'{e.code} - {e.msg}', file=sys.stderr)
        print(
            f'{json.dumps(e.read().decode("utf-8"), indent=2, sort_keys=True, ensure_ascii=False)}')
        err = e
    except ValueError as e:
        print(e, file=sys.stderr)
        err = e
    finally:
        if err:
            sys.exit(1)
