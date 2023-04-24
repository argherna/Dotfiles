;;
;; init.el
;;
;; Started December 2010, Version 23.1.1
;; 
;; Andy Gherna (argherna@gmail.com)
;;
;; CHANGELOG:
;;
;; <2022-11-30> Some long overdue updates:
;;              * Remove cl package
;;              * Delete commented lisp
;;
;; <2014-01-01> remove global-hl-line-mode
;;              allow tabs (remember to use M-x untabify)
;;
;; <2012-08-08> Changed to GTK-IDE color theme in color-theme-load.el
;;              Added arrange-frame function to "remember" the window size
;;
;; <2012-05-28> added duplicate-line function
;;
;; <2011-03-31> org-mode/remember mode added
;; 
;; <2011-04-04> moved mode-specific configuration to separate files (per mode)
;;


;; -----------------------------------------------------------------------------
;;                              load-path setup
;; -----------------------------------------------------------------------------

(let ((default-directory "~/.emacs.d/site-lisp/"))
      (normal-top-level-add-to-load-path '("."))
      (normal-top-level-add-subdirs-to-load-path))
;(add-to-list 'load-path "~/.emacs.d/elpa/s-20140620.1657")
;(add-to-list 'load-path "~/.emacs.d/elpa/darcula-them-20141211.128")
(add-to-list 'auto-mode-alist '("\\.js\\'" . js2-mode))

;; -----------------------------------------------------------------------------
;;                              interface setup
;; -----------------------------------------------------------------------------

;; Startup customizations
;;
(setq inhibit-startup-message t)

;; Disable initial scratch message
;;
(setq initial-scratch-message nil)

;; Move the scroll bar to the right (why would you want it on the the left?)
;;
(when (display-graphic-p)
  (set-scroll-bar-mode 'right))

;; Sets the line-width
;;
(setq-default fill-column 80)

;; Turn on line numbering.
;;
(when (>= emacs-major-version 23)
  (require 'linum)
  (global-linum-mode 0)
)

;; Line and column numbers in the command bar.
;;
(line-number-mode 1)
(column-number-mode 1)

;; Use spaces for indents
;; 
;;(setq-default indent-tabs-mode nil)

;; Set tab defaults
;;
(setq tab-width 4)

;; Show matching parenthesis
;;
(show-paren-mode 1)

;; Relocate where emacs writes backup files (keeps the ~ files out of your
;; working directory).
;;
;; From <URL:http://www.emacswiki.org/emacs/BackupDirectory>
(setq
  backup-by-copying t 
  backup-directory-alist
   '(("." . "~/.emacs.d/backups"))
  delete-old-versions t
  kept-new-versions 6
  kept-old-versions 2
  version-control t)

;; Start emacs as a server, using emacsclient to attach to this running emacs.
;;
(server-start)
(remove-hook 'kill-buffer-query-functions 'server-kill-buffer-query-function)

;; Set desktop-save
;;
(desktop-save-mode 1)

;; Enable X-clipboard
;;
(setq x-select-enable-clipboard t)

;; Mouse in terminal
;;
(unless window-system
  (xterm-mouse-mode 1)
  (global-set-key [mouse-4] '(lambda () 
                               (interactive)
                               (scroll-down 1)))

  (global-set-key [mouse-5] '(lambda () 
                               (interactive)
                               (scroll-up 1)))
)

;; Overwrite selected text.
;;
(delete-selection-mode 1)

;; Ido mode
;;
(setq ido-enable-flex-matching t)
(setq ido-everywhere t)
(ido-mode 1) 


;; -----------------------------------------------------------------------------
;;
;;                        Custom variable definitions
;;
;; -----------------------------------------------------------------------------

;; Autoindent andy/open-*-line functions.
1;;
(defvar newline-and-indent t
  "Modify the behavior of the andy/open-*-line functions causing them to be 
   autoindented.")

(defvar scroll-preserve-screen-position)

;; Control cursor blink.
;;
(blink-cursor-mode 0)

;; -----------------------------------------------------------------------------
;;
;;                                 Functions
;;
;; -----------------------------------------------------------------------------

;; Behave like vi's o command
;;
(defun andy/open-next-line (arg)
  "Move to the next line and then opens a line."
  (interactive "p")
  (end-of-line)
  (open-line arg)
  (next-line 1)
  (when newline-and-indent
    (indent-according-to-mode)))

;; Behave like vi's O command
;;
(defun andy/open-previous-line (arg)
  "Open a new line before the current line."
  (interactive "p")
  (beginning-of-line)
  (open-line arg)
  (when newline-and-indent
    (indent-according-to-mode)))

;; Scroll down 1 line leaving the point where it is.
;;
(defun andy/scroll-1-down ()
  "Scroll down one line."
  (interactive)
  (let ((scroll-preserve-screen-position t))
    (scroll-down 1)))

;; Scroll up 1 line leaving the point where it is.
;;
(defun andy/scroll-1-up ()
  "Scroll up one line."
  (interactive)
  (let ((scroll-preserve-screen-position t))
    (scroll-up 1)))

;; Duplicates the current line or region arg times.
;;
;; From:
;; <URL:http://tuxicity.se/emacs/elisp/2010/03/11/duplicate-current-line-or-region-in-emacs.html>
;;
(defun andy/duplicate-current-line-or-region (arg)
  "Duplicates the current line or region ARG times.
If there's no region, the current line will be duplicated. However, if
there's a region, all lines that region covers will be duplicated."
  (interactive "p")
  (let (beg end (origin (point)))
    (if (and mark-active (> (point) (mark)))
        (exchange-point-and-mark))
    (setq beg (line-beginning-position))
    (if mark-active
        (exchange-point-and-mark))
    (setq end (line-end-position))
    (let ((region (buffer-substring-no-properties beg end)))
      (dotimes (i arg)
        (goto-char end)
        (newline)
        (insert region)
        (setq end (point)))
      (goto-char (+ origin (* (length region) arg) arg)))))

;; Moves a region.
;;
(defun andy/move-text-internal (arg)
   (cond
    ((and mark-active transient-mark-mode)
     (if (> (point) (mark))
            (exchange-point-and-mark))
     (let ((column (current-column))
              (text (delete-and-extract-region (point) (mark))))
       (forward-line arg)
       (move-to-column column t)
       (set-mark (point))
       (insert text)
       (exchange-point-and-mark)
       (setq deactivate-mark nil)))
    (t
     (beginning-of-line)
     (when (or (> arg 0) (not (bobp)))
       (forward-line)
       (when (or (< arg 0) (not (eobp)))
            (transpose-lines arg))
       (forward-line -1)))))

;; Moves a region down.
;;
(defun andy/move-text-down (arg)
   "Move region (transient-mark-mode active) or current line
  arg lines down."
   (interactive "*p")
   (andy/move-text-internal arg))

;; Moves a region up.
;;
(defun andy/move-text-up (arg)
   "Move region (transient-mark-mode active) or current line
  arg lines up."
   (interactive "*p")
   (andy/move-text-internal (- arg)))

;; Compute the length of the marked region 
;;
(defun andy/region-length ()
  "Length of a region"
  (interactive)
  (message (format "%d" (- (region-end) (region-beginning)))))


(defun andy/hardcore-ui () 
  "Turn off line numbers, toolbar, menu, scroll bar, highlight parenthesis."
  (interactive)
  (if (>= emacs-major-version 23)
    (global-linum-mode 0))
  (menu-bar-mode 0)
  (show-paren-mode 1)
  (when (display-graphic-p)
    (tool-bar-mode 0)
    (scroll-bar-mode 0))
)

;; Create a new empty buffer named '-untitled-n-' where n is some digit
;; starting with 0.
;; 
(defun andy/new-empty-buffer ()
  (interactive)
  (let ((n 0)
        new-buf)
    (while (progn
             (setq new-buf (concat "-untitled-"
                                       (if (= n 0) "0" (int-to-string n))
                                       "-"))
             (incf n)
             (get-buffer new-buf)))
    (switch-to-buffer (get-buffer-create new-buf))
    (text-mode)))

;; Increments the number at the point.
;;
(defun andy/increment-number-at-point ()
  (interactive)
  (skip-chars-backward "0123456789")
  (or (looking-at "[0123456789]+")
    (error "No number at point"))
    (replace-match (number-to-string (1+ (string-to-number (match-string 0))))))


;; -----------------------------------------------------------------------------
;;
;;                             Custom keybindings
;;
;; -----------------------------------------------------------------------------


(when (display-graphic-p)
  (global-set-key (kbd "C-c s") 'scroll-bar-mode)
  (global-set-key (kbd "C-c t") 'tool-bar-mode)
)

(global-set-key (kbd "C-c +") 'andy/increment-number-at-point)
(global-set-key (kbd "C-c o") 'andy/open-next-line)
(global-set-key (kbd "C-c O") 'andy/open-previous-line)
(global-set-key (kbd "M-n") 'andy/scroll-1-up)
(global-set-key (kbd "M-p") 'andy/scroll-1-down)
(global-set-key (kbd "C-x <down>") 'andy/duplicate-current-line-or-region)
(global-set-key (kbd "C-x <up>") 'andy/duplicate-current-line-or-region)
(global-set-key (kbd "<f5>") 'andy/move-text-down)
(global-set-key (kbd "<f6>") 'andy/move-text-up)
(global-set-key (kbd "<f7>") 'kill-this-buffer)
(global-set-key (kbd "<f8>") 'andy/new-empty-buffer)
(global-set-key (kbd "<f9>") 'kill-whole-line)

(global-set-key (kbd "C-c b") 'ibuffer)
(when (>= emacs-major-version 23)
  (global-set-key (kbd "C-c C-c l") 'global-linum-mode)
  (global-set-key (kbd "C-c C-l") 'linum-mode)
)
(global-set-key (kbd "C-c C-c r") 'revert-buffer)

(global-set-key (kbd "C-c <left>")  'windmove-left)
(global-set-key (kbd "C-c <right>") 'windmove-right)
(global-set-key (kbd "C-c <up>")    'windmove-up)
(global-set-key (kbd "C-c <down>")  'windmove-down)

(global-set-key (kbd "C-c w") 'write-region)

;; -----------------------------------------------------------------------------
;;
;;                             Execute functions
;;
;; -----------------------------------------------------------------------------


;; Run these when in graphics mode.
;;

(when (display-graphic-p)
  (defun arrange-frame (w h x y)
    "Set the width, height, and x/y position of the current frame"
    (let ((frame (selected-frame)))
      (delete-other-windows)
      (set-frame-position frame x y)
      (set-frame-size frame w h)))
  (arrange-frame 120 40 5 5)
)

(andy/hardcore-ui)

;; -----------------------------------------------------------------------------
;;
;;                            Common Abbreviations
;;
;; -----------------------------------------------------------------------------

;; Inspired by <http://ergoemacs.org/emacs/emacs_abbrev_mode.html>
(define-abbrev-table 'global-abbrev-table '(

  ;; math/unicode symbols
  ("8rarr" "→")
  ("8larr" "←")
  ("8uarr" "↑")
  ("8darr" "↓")
  ("8lrarr" "↔")
  ("8udarr" "↕")
  
  ("8in" "∈")
  ("8nin" "∉")
  ("8inf" "∞")
  ("8luv" "♥")
  ("8smly" "☺")

  ;; email
  ("8me" "argherna@gmail.com")

))

;; stop asking whether to save newly added abbrev when quitting emacs
(setq save-abbrevs nil)

;; turn on abbrev mode globally
(setq-default abbrev-mode t)


;; -----------------------------------------------------------------------------
;;
;;                           DO NOT EDIT BELOW HERE
;;
;; -----------------------------------------------------------------------------

(put 'narrow-to-region 'disabled nil)
(put 'upcase-region 'disabled nil)
(put 'erase-buffer 'disabled nil)
