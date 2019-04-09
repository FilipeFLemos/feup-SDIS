osascript -e 'tell app "Terminal"
    do script "cd ~/FEUP/SDIS/SDIS/
                sh reset.sh
                sh rmi.sh"
end tell'

sleep 1

osascript -e 'tell app "Terminal"
    do script "cd ~/FEUP/SDIS/SDIS/
                sh run.sh 1.0 1"
end tell'

osascript -e 'tell app "Terminal"
    do script "cd ~/FEUP/SDIS/SDIS/
                sh run.sh 1.0 2"
end tell'

osascript -e 'tell app "Terminal"
    do script "cd ~/FEUP/SDIS/SDIS/
                sh run.sh 1.0 3"
end tell'

osascript -e 'tell app "Terminal"
    do script "cd ~/FEUP/SDIS/SDIS/
                sh run.sh 1.0 4"
end tell'

