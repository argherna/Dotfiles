# -----------------------------------------------------------------------------
# 
# aws-functions.sh
#
# Collection of functions and aliases to use when running AWS operations.
#
# -----------------------------------------------------------------------------


# ------------------------------------------------------------------------------
#
#                                AWS Functions
#
# ------------------------------------------------------------------------------

login_ecr() {
  if [[ $# -ne 1 ]]; then
    cat <<-ENDOFHELP
	Logs a user in to ECR in the specified region.

	Usage: $FUNCNAME <region-name>

	  <region-name>  AWS region to log in to.

	These are valid regions:

	  us-east-1 (N. Virginia)
	  us-east-2 (Ohio)
	  us-west-1 (N. California)
	  us-west-2 (Oregon)
ENDOFHELP
    return 1
  fi

  local REGION=$1

  $(aws ecr get-login --no-include-email --region $REGION)
}
