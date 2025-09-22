document.addEventListener("DOMContentLoaded", () => {
  const potEl = document.getElementById("pot");
  const communityCardsEl = document.getElementById("communityCards");
  const playersEl = document.getElementById("players");
  const logEl = document.getElementById("log");

  function log(message) {
    if (!message) return;
    const p = document.createElement("p");
    p.textContent = message;
    logEl.appendChild(p);
    logEl.scrollTop = logEl.scrollHeight;
  }

  function animatePot() {
    potEl.classList.add("bump");
    setTimeout(() => potEl.classList.remove("bump"), 300);
  }

  function renderCommunityCards(cards) {
    communityCardsEl.innerHTML = "";
    if (!cards) return;
    cards.forEach((c, i) => {
      const div = document.createElement("div");
      div.className = "card";
      div.textContent = `${c.rank}${c.suit}`;
      communityCardsEl.appendChild(div);
      setTimeout(() => div.classList.add("show"), i * 300);
    });
  }

  function renderPlayers(players, yourHand, yourTurn) {
    playersEl.innerHTML = "";
    players.forEach(p => {
      const div = document.createElement("div");
      div.className = "player";
      if (yourTurn && p.name === "You") div.classList.add("active");

      const hand = p.name === "You"
        ? yourHand?.map(c => `${c.rank}${c.suit}`).join(" ")
        : p.hand ? p.hand.map(c => `${c.rank}${c.suit}`).join(" ") : "";

      div.innerHTML = `
        <div><strong>${p.name}</strong></div>
        <div>Фишки: ${p.chips}</div>
        <div>${hand}</div>
        ${p.combo ? `<div><em>${p.combo}</em></div>` : ""}
      `;
      playersEl.appendChild(div);
    });
  }

  async function updateGame() {
    try {
      const res = await fetch("/api/state");
      const data = await res.json();
      if (!data.success) {
        log("Ошибка: " + data.error);
        return;
      }

      potEl.textContent = "Банк: " + data.pot;
      animatePot();
      renderCommunityCards(data.communityCards);
      renderPlayers(data.players, data.hand, data.yourTurn);

      log(data.message);

      if (data.roundEnded && data.winners.length > 0) {
        log("Раунд завершён. Победители: " + data.winners.join(", "));
        setTimeout(() => fetch("/api/continue", { method: "POST" }).then(updateGame), 3000);
      }
    } catch (e) {
      log("Ошибка связи с сервером");
    }
  }

  // Кнопки действий
  document.getElementById("foldBtn").addEventListener("click", () => {
    fetch("/api/action", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ action: "FOLD" })
    }).then(updateGame);
  });

  document.getElementById("checkBtn").addEventListener("click", () => {
    fetch("/api/action", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ action: "CHECK" })
    }).then(updateGame);
  });

  document.getElementById("betBtn").addEventListener("click", () => {
    const amount = parseInt(document.getElementById("betAmount").value, 10);
    fetch("/api/action", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ action: "BET", amount })
    }).then(updateGame);
  });

  // Старт игры (с ботами!)
  fetch("/api/start", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      gameType: "texas",
      deckType: "standard",
      smallBlind: 10,
      bigBlind: 20,
      players: [
        { type: "human", name: "You" },
        { type: "random", name: "Bot1" },
        { type: "ai", name: "Bot2" }
      ]
    })
  }).then(updateGame);

  setInterval(updateGame, 2000);
});
