Contributing
============

* Do not commit on master, only on your branch. 
* Rebase before merging onto master (only fast forward
  merges to master).
* Cleanup your commit history if necessary to provide good commit messages (`git rebase -i`)

```
git checkout -b <your_branch>
git add {YOUR FILES}
git commit -m "your message"
git checkout master
git pull
git checkout <your_branch>
git rebase <your_branch> master
git checkout master
git merge <your_branch>
git push
git branch -d <your_branch>
```
