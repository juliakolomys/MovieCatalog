const form = document.getElementById('search-form');
const input = document.getElementById('search-input');
const statusDiv = document.getElementById('status');
const resultsDiv = document.getElementById('results');
const suggestionsList = document.getElementById('search-suggestions');


const params = new URLSearchParams(window.location.search);
const directorName = params.get("name");


if (directorName) {
    document.title = `Фільми режисера: ${directorName}`;
    loadMoviesByDirector(directorName);
} else {

    form.addEventListener('submit', async (ev) => {
        ev.preventDefault();
        await doSearch();
    });

    input.addEventListener('input', async () => {
        const q = input.value.trim();
        suggestionsList.innerHTML = '';
        if (q.length < 2) return;

        try {
            const resp = await fetch(`/api/suggest?q=${encodeURIComponent(q)}`);
            const suggestions = await resp.json();

            suggestions.forEach(title => {
                const li = document.createElement('li');
                li.textContent = title;
                li.addEventListener('click', () => {
                    input.value = title;
                    suggestionsList.innerHTML = '';
                    doSearch();
                });
                suggestionsList.appendChild(li);
            });
        } catch (err) {
            console.error('Suggest error', err);
        }
    });

    input.addEventListener('blur', () => {
        setTimeout(() => suggestionsList.innerHTML = '', 200);
    });
}


async function doSearch() {
    const q = input.value.trim();
    statusDiv.textContent = '';
    resultsDiv.innerHTML = '';

    if (!q) {
        statusDiv.textContent = 'Будь ласка, введіть пошуковий запит.';
        resultsDiv.innerHTML = '<div class="empty">Введіть назву фільму та натисніть «Пошук».</div>';
        return;
    }

    resultsDiv.innerHTML = '<div class="empty">Завантаження...</div>';

    try {
        const resp = await fetch(`/api/search?q=${encodeURIComponent(q)}`, { method: 'GET' });
        const data = await resp.json();

        if (data.status === 'error') {
            statusDiv.textContent = data.message || 'Помилка';
            resultsDiv.innerHTML = `<div class="empty">${data.message || 'Помилка при отриманні результатів.'}</div>`;
            return;
        }

        if (data.status !== 'ok' || !data.movie) {
            resultsDiv.innerHTML = '<div class="empty">Фільми не знайдено.</div>';
            return;
        }

        const mainHtml = document.createElement('div');
        mainHtml.className = 'result-card';

        const header = document.createElement('div');
        header.className = 'result-header';

        const title = document.createElement('h2');
        title.className = 'title';
        title.textContent = data.movie.title || 'Без назви';

        const scoreSpan = document.createElement('div');
        scoreSpan.className = 'score';
        scoreSpan.textContent = (typeof data.score === 'number') ? `Релевантність: ${data.score.toFixed(4)}` : 'Релевантність: N/A';

        header.appendChild(title);
        header.appendChild(scoreSpan);
        mainHtml.appendChild(header);

        const meta = document.createElement('div');
        meta.className = 'meta';
        const genresText = Array.isArray(data.movie.genres) ? data.movie.genres.join(', ') : '';
        const directors = Array.isArray(data.movie.directors) ? data.movie.directors : [];

        const directorLinks = directors.map(name => {
            const encoded = encodeURIComponent(name);
            return `<a href="/director.html?name=${encoded}" class="director-link">${name}</a>`;
        }).join(', ');

        meta.innerHTML = `
            <strong>Жанри:</strong> ${genresText || '—'}<br/>
            <strong>Режисер:</strong> ${directorLinks || '—'}
        `;
        mainHtml.appendChild(meta);

        if (data.movie.description) {
            const desc = document.createElement('div');
            desc.className = 'desc';
            desc.textContent = data.movie.description;
            mainHtml.appendChild(desc);
        }

        resultsDiv.innerHTML = '';
        resultsDiv.appendChild(mainHtml);

        const recHeader = document.createElement('h3');
        recHeader.style.marginTop = '14px';
        recHeader.textContent = 'Схожі фільми:';
        resultsDiv.appendChild(recHeader);

        if (!Array.isArray(data.recommendations) || data.recommendations.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'empty';
            empty.textContent = 'Схожих фільмів не знайдено.';
            resultsDiv.appendChild(empty);
        } else {
            const recContainer = document.createElement('div');
            recContainer.className = 'rec-list';

            data.recommendations.forEach(r => {
                const itemDiv = document.createElement('div');
                itemDiv.className = 'rec-item';

                const rt = document.createElement('p');
                rt.className = 'rec-title';
                rt.textContent = r.movie?.title || 'Назва відсутня';

                const rm = document.createElement('p');
                rm.className = 'rec-meta';
                const genres = Array.isArray(r.movie?.genres) ? r.movie.genres.join(', ') : '';
                rm.innerHTML = `<strong>Жанри:</strong> ${genres || '—'} &nbsp; • &nbsp; <strong>Score:</strong> ${typeof r.score === 'number' ? r.score.toFixed(4) : 'N/A'}`;

                itemDiv.appendChild(rt);
                itemDiv.appendChild(rm);

                recContainer.appendChild(itemDiv);
            });

            resultsDiv.appendChild(recContainer);
        }

    } catch (err) {
        console.error('Search error', err);
        statusDiv.textContent = `Помилка: ${err.message}`;
        resultsDiv.innerHTML = '<div class="empty">Помилка при отриманні результатів.</div>';
    }
}


async function loadMoviesByDirector(name) {
    const resp = await fetch(`/api/director?name=${encodeURIComponent(name)}`);
    const movies = await resp.json();

    if (!Array.isArray(movies) || movies.length === 0) {
        resultsDiv.innerHTML = `<div class="empty">Фільми не знайдено для режисера "${name}".</div>`;
        return;
    }

    resultsDiv.innerHTML = '';
    movies.forEach(m => {
        const card = document.createElement("div");
        card.className = "result-card";

        const title = document.createElement("h2");
        title.className = "title";
        title.textContent = m.title || "Без назви";

        const meta = document.createElement("div");
        meta.className = "meta";
        const genres = Array.isArray(m.genres) ? m.genres.join(", ") : "";
        meta.innerHTML = `<strong>Жанри:</strong> ${genres || "—"} &nbsp; • &nbsp; <strong>Рік:</strong> ${m.year || "—"}`;

        card.appendChild(title);
        card.appendChild(meta);

        if (m.description) {
            const desc = document.createElement("div");
            desc.className = "desc";
            desc.textContent = m.description;
            card.appendChild(desc);
        }

        resultsDiv.appendChild(card);
    });
}
