" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
" File:        _vimrc
" Author:    Andy Gherna <argherna@gmail.com>
" Date:        August 2000
" Changes:
" Aug 2007:   All vim specific settings.  This file should not have
"             any settings related to gvim.
" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
" ====================================================================
" Editor settings.
" ====================================================================
set nocompatible
set autoindent        " Autoindent on.
set joinspaces        " Join two lines adding two spaces after a '.'.
set laststatus=2    " Show the last command.
set ch=2            " Command line is 2 lines high.
set et              " Soft tabs
set ruler            " Show the ruler.
set shellredir=2>   " Shell redirection.
set shiftwidth=4    " Shift text only 4 spaces.
set showcmd            " Show the last command executed.
set showmatch        " Show the matching bracket for the last ')'.
set showmode        " Show the current mode.
"set sw=4
set smarttab        " Smart tabs on.
set tabstop=4        " Tab 4 spaces only.
"set tabstop=8        " Tab 8 spaces only.
set softtabstop=4
set shiftwidth=4
set noexpandtab
"set list
"set listchars=tab:.\ 
"execute 'set listchars=tab:'.nr2char(187).nr2char(183)
"set tw=80           " Wrap at 80 characters.
"set nu                " Show line numbers.
set nowrap          " No wrapping, please.
set nobackup

syntax enable

"source $VIM/_vimrc

"----- set up the stuff for color highlighing in an xterm
if &term =~ "xterm"
 if has("terminfo")
  set t_Co=16
  set t_Sf=[3%p1%dm
  set t_Sb=[4%p1%dm
  set t_vb=
 else
  set t_Co=16
  set t_Sf=[3%dm
  set t_Sb=[4%dm
  set t_vb=
 endif
endif

" ====================================================================
" Key Mappings.
" ====================================================================
" --------------------------------------------------------------------
" VIM mappings.
" Reserve F2, F3, and F4 for configuring VIM.
" Reserve F9, F10 for common functions.
" --------------------------------------------------------------------
" Open help for the current word.
nmap <S-F1> :he <C-R>=expand("<cword>")<cr><cr>

" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
" F2
" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
" Command mode mappings.
" Edit _gvimrc in the current buffer.
nmap <F2> :e $HOME/.gvimrc <CR>
" Open _gvimrc in another buffer and give the buffer it's max height.
nmap <S-F2> :sp $HOME/.gvimrc<CR><C-S-W>_<CR>
" Re-read this file to activate new configurations.
nmap <C-F2> :so $HOME/.gvimrc <CR>
" Edit _vimrc in the current buffer.
nmap <C-S-F2> :e $HOME/.vimrc <CR>
" Open _vimrc in another buffer and give the buffer it's max height.
nmap <M-F2> :sp $HOME/.vimrc<CR><C-S-W>_<CR>
" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
" F3
" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
" Command mode mappings.
" Edit a file under $VIMRUNTIME in the current buffer.
" Source the current buffer (assuming it is a VIM script!)
nmap <C-S-F3> :so <C-R>=bufname("%")<cr><cr>
" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
" F4
" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
" Normal mode mappings.
" Turn off the highlighted search.
nmap <F4> :nohls<cr>
" Parenthesize the current word.
nmap <S-F4> maa0<ESC>mbbma$a x<ESC>`awgebi(<ESC>ea)<ESC>$xx`blx`a
" Quote the current word.
nmap <C-F4> maa0<ESC>mbbma$a x<ESC>`awgebi"<ESC>ea"<ESC>$xx`blx`a
" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
" F9 - Add/Remove/Swap buffers.
" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
nmap <F7> :bn!<cr>
nmap <S-F7> :bp!<cr>
nmap <C-F7> :bd!<cr>
nmap <M-F7> :bp!<cr>
nmap <C-S-F7> :bad
" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
" Additional key mappings.
" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
" Repeat (copy and paste below) the current line.
nmap =R Yp
omap =R Yp
vmap =R yp
" Swap two Words.
nmap =W dWelp
" Capitalize/uncapitalize a Word.
"nmap =C b~
nmap <F11> b~
" ===================================================================
" User-defined commands
" ===================================================================
" Repeat a block of text
command! -range R <line1>,<line2>co<line2>

