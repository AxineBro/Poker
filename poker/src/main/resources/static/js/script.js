let currentState = {};

function logMessage(msg) {
    const log = document.getElementById('game-log');
    if (log) {
        log.innerHTML += `<p>${msg}</p>`;
        log.scrollTop = log.scrollHeight;
    }
}

let players = [{ type: 'human', name: 'You' }];

function addPlayer(type) {
    const name = type === 'random' ? 'Bot Random ' + (players.length) : 'Bot AI ' + (players.length);
    players.push({ type, name });
    updatePlayersList();
}

function updatePlayersList() {
    const list = document.getElementById('players-list');
    if (list) {
        list.innerHTML = '';
        players.slice(1).forEach(p => {
            const div = document.createElement('div');
            div.textContent = p.name;
            div.className = 'player-name';
            list.appendChild(div);
        });
    }
}

function startGame() {
    const gameType = document.getElementById('game-type').value;
    const deckType = document.getElementById('deck-type').value;
    const smallBlind = document.getElementById('small-blind').value;
    const bigBlind = document.getElementById('big-blind').value;

    fetch('/api/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ gameType, deckType, players, smallBlind, bigBlind })
    }).then(res => res.json())
      .then(data => {
          console.log('Ответ /api/start:', data);
          if (data.success) {
              window.location.href = '/game.html';
          } else {
              logMessage('Ошибка запуска: ' + (data.error || 'Неизвестная ошибка'));
              alert('Ошибка запуска: ' + (data.error || 'Неизвестная ошибка'));
          }
      }).catch(err => {
          console.error('Ошибка соединения /api/start:', err);
          logMessage('Ошибка соединения: ' + err.message);
          alert('Ошибка соединения: ' + err.message);
      });
}

function loadGameState() {
    fetch('/api/state')
        .then(res => res.json())
        .then(data => {
            console.log('Ответ /api/state:', data);
            if (!data.success) {
                logMessage('Ошибка состояния: ' + (data.error || 'Неизвестная ошибка сервера'));
                return;
            }
            currentState = data;
            document.getElementById('pot').textContent = data.pot || 0;
            document.getElementById('to-call').textContent = data.toCall || 0;
            renderCommunityCards(data.communityCards || []);
            renderPlayers(data.players || []);
            renderHand(data.hand || []);
            logMessage(data.message || '');

            if (data.yourTurn) {
                document.getElementById('actions').style.display = 'flex';
                document.getElementById('raise-amount').value = '';
            } else {
                document.getElementById('actions').style.display = 'none';
                if (!data.roundEnded) {  // Stop refresh if round ended
                    setTimeout(loadGameState, 1500);
                }
            }

            if (data.roundEnded) {
                showWinnerModal(data.winners || [], data.players || []);
            } else {
                document.getElementById('winner-modal').style.display = 'none';
            }
        }).catch(err => {
            console.error('Ошибка загрузки состояния:', err);
            logMessage('Ошибка загрузки состояния: ' + err.message);
        });
}

function renderCommunityCards(cards) {
    const container = document.getElementById('community-cards');
    if (container) {
        container.innerHTML = '';
        if (Array.isArray(cards)) {
            cards.forEach(card => {
                if (card && card.rank && card.suit) {
                    const div = document.createElement('div');
                    div.className = 'card';
                    div.style.backgroundImage = `url('/images/cards/${card.rank}${card.suit}.svg')`;
                    container.appendChild(div);
                }
            });
        }
    }
}

function renderPlayers(playersData) {
    const container = document.getElementById('players-circle');
    if (container) {
        container.innerHTML = '';
        if (Array.isArray(playersData) && playersData.length > 0) {
            // Найти индекс human
            const humanIndex = playersData.findIndex(p => p.name === 'You');
            const numPlayers = playersData.length;
            const angleStep = 360 / numPlayers;
            const humanAngle = 270; // Bottom
            playersData.forEach((p, i) => {
                if (p && p.name && p.chips != null) {
                    const relativeIndex = (i - humanIndex + numPlayers) % numPlayers;
                    const angle = humanAngle + relativeIndex * angleStep;
                    const div = document.createElement('div');
                    div.className = 'player';
                    if (p.folded) div.classList.add('folded');
                    div.innerHTML = `<p>${p.name}</p><p>${p.chips}</p><p>Bet: ${p.bet || 0}</p>`;
                    if (currentState.roundEnded && p.hand) {
                        const handDiv = document.createElement('div');
                        handDiv.className = 'player-hand';
                        p.hand.forEach(card => {
                            const cardDiv = document.createElement('div');
                            cardDiv.className = 'card small';
                            cardDiv.style.backgroundImage = `url('/images/cards/${card.rank}${card.suit}.svg')`;
                            handDiv.appendChild(cardDiv);
                        });
                        div.appendChild(handDiv);
                        if (p.combo) {
                            const comboP = document.createElement('p');
                            comboP.className = 'player-combo';
                            comboP.textContent = p.combo;
                            div.appendChild(comboP);
                        }
                    }
                    const radiusX = 400; // Horizontal radius for oval
                    const radiusY = 250; // Vertical radius
                    div.style.left = `calc(50% + ${radiusX * Math.cos((angle * Math.PI) / 180)}px)`;
                    div.style.top = `calc(50% + ${radiusY * Math.sin((angle * Math.PI) / 180)}px)`;
                    container.appendChild(div);
                }
            });
        }
    }
}

function renderHand(hand) {
    const container = document.getElementById('hand-cards');
    if (container) {
        container.innerHTML = '';
        if (Array.isArray(hand)) {
            hand.forEach(card => {
                if (card && card.rank && card.suit) {
                    const div = document.createElement('div');
                    div.className = 'card';
                    div.style.backgroundImage = `url('/images/cards/${card.rank}${card.suit}.svg')`;
                    container.appendChild(div);
                }
            });
        }
    }
}

function performAction(action) {
    let amount = 0;
    if (action === 'raise') {
        action = 'bet';
        amount = parseInt(document.getElementById('raise-amount').value) || 0;
        if (amount <= 0) {
            logMessage('Ошибка: сумма рейза должна быть больше 0');
            alert('Сумма рейза должна быть больше 0');
            return;
        }
        amount = currentState.currentBet + amount; // Total bet = currentBet + raise amount
    }
    fetch('/api/action', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action, amount })
    }).then(res => res.json())
      .then(data => {
          console.log('Ответ /api/action:', data);
          if (!data.success) {
              logMessage('Ошибка действия: ' + (data.error || 'Неизвестная ошибка'));
              alert('Ошибка действия: ' + (data.error || 'Неизвестная ошибка'));
          }
          document.getElementById('raise-amount').value = '';
          loadGameState();
      }).catch(err => {
          console.error('Ошибка соединения /api/action:', err);
          logMessage('Ошибка соединения: ' + err.message);
          alert('Ошибка соединения: ' + err.message);
      });
}

function showWinnerModal(winners, players) {
    const modal = document.getElementById('winner-modal');
    const winnersText = document.getElementById('winners-text');
    const showdownDetails = document.getElementById('showdown-details');
    if (modal && winnersText && showdownDetails) {
        winnersText.textContent = Array.isArray(winners) && winners.length > 0 ? 'Победители: ' + winners.join(', ') : 'Нет победителей';
        showdownDetails.innerHTML = '';
        // Show only winners' combos
        players.forEach(p => {
            if (p.combo && !p.folded && winners.includes(p.name)) {
                const detail = document.createElement('p');
                detail.textContent = `${p.name}: ${p.combo}`;
                showdownDetails.appendChild(detail);
            }
        });
        modal.style.display = 'flex';
    }
}

function continueGame() {
    fetch('/api/continue', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    }).then(res => res.json())
      .then(data => {
          console.log('Ответ /api/continue:', data);
          if (!data.success) {
              logMessage('Ошибка продолжения: ' + (data.error || 'Неизвестная ошибка'));
              alert('Ошибка продолжения: ' + (data.error || 'Неизвестная ошибка'));
              return;
          }
          document.getElementById('winner-modal').style.display = 'none';
          loadGameState();
      }).catch(err => {
          console.error('Ошибка соединения /api/continue:', err);
          logMessage('Ошибка соединения: ' + err.message);
          alert('Ошибка соединения: ' + err.message);
      });
}