#!/bin/bash

# Installs rust and the toolchain
#
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Installs rust-built tools:
#
#  ripgrep (rg): fast grep replacement
#  exa: ls replacement with tree views built-in
#  bat: cat with syntax highlighting and margins
#  xh: HTTP requests like curl & httpie
#  git-delta: git extension
#  broot: directory browser in terminal
#  ag: a.k.a. the silver searcher
#
cargo install ripgrep
cargo install exa
cargo install --locked bat
cargo install xh
cargo install git-delta
cargo install broot
cargo install ag
