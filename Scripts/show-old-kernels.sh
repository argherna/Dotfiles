dpkg -l | tail -n +6 | grep -E 'linux-image-[0-9]+' | grep -Fv $(uname -r)
