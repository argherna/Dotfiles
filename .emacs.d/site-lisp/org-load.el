;;
;; org-load.el
;;
;; Started April 2011
;;
;; Andy Gherna (argherna@gmail.com)
;;
;; org-mode specific settings.  This file is included in ~/.emacs.d/site-lisp
;; and allows toggling on and off tramp-mode settings from that file.
;;
;; CHANGELOG
;; <2011-04-04> Initial version
;; 
;; <Tue Dec 31 17:51:32 CST 2013> Update for version 8.x, set up capture mode,
;; 
;; <Wed Feb  4 08:01:44 CST 2015> Removed a bunch of commented settings that
;;                                are no longer needed.


;; -----------------------------------------------------------------------------
;;                                   setup
;; -----------------------------------------------------------------------------

(provide 'org-load)

;; keybinding for agenda
;;
(define-key global-map "\C-ca" 'org-agenda)

;; path to agenda files
;;
(defvar org-agenda-home)
(setq org-agenda-home 
      (concat (expand-file-name "~") "/Dropbox/Personal/Documents/GTD/"))

(defvar org-current (concat org-agenda-home "current.org"))
(defvar org-journal (concat org-agenda-home "journal.org"))
(defvar org-notebk1 (concat org-agenda-home "notebook-1.org"))

;; basic remember configuration
;; see <URL:http://members.optusnet.com.au/~charles57/GTD/remember.html>
;;
(setq org-directory org-agenda-home)
(setq org-default-notes-file org-journal)

;; keybinding for capture
;;
(define-key global-map "\C-cr" 'org-capture)

;; remember templates
;;
(setq org-capture-templates
 '(
   ("t" "Todo" entry (file+headline org-current "Incoming") "* TODO %^{Brief Description} %^g\n  %?\n  Added: %U")
   ("j" "Journal" entry (file+headline org-journal "Entries") "\n* %^{Topic} %T %^g\n%i  %?")
   ("n" "Notebook" entry (file+headline org-notebk1 "Notes") "\n* %^{Topic} %T %^g\n%i  %?")
  )
)
