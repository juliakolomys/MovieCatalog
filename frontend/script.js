

const form = document.getElementById('search-form');
const input = document.getElementById('search-input');
const statusDiv = document.getElementById('status');
const resultsDiv = document.getElementById('results');

form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    await doSearch();
});

input.addEventListener('keydown', (ev) => {
    if (ev.key === 'Enter') {
    }
});

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
        if (!resp.ok) {
            const text = await resp.text();
            throw new Error(`HTTP ${resp.status}: ${text}`);
        }
        const data = await resp.json();

        if (!Array.isArray(data) || data.length === 0) {
            resultsDiv.innerHTML = '<div class="empty">Фільми не знайдено за вашим запитом.</div>';
            return;
        }


        const item = data[0];
        const main = item.movie || null;
        const mainScore = (typeof item.score === 'number') ? item.score : null;
        const recs = Array.isArray(item.recommendations) ? item.recommendations : [];

        if (!main) {
            resultsDiv.innerHTML = '<div class="empty">Сервер повернув некоректні дані.</div>';
            return;
        }

        const mainHtml = document.createElement('div');
        mainHtml.className = 'result-card';

        const header = document.createElement('div');
        header.className = 'result-header';

        const title = document.createElement('h2');
        title.className = 'title';
        title.textContent = main.title || 'Без назви';

        const scoreSpan = document.createElement('div');
        scoreSpan.className = 'score';
        scoreSpan.textContent = (typeof mainScore === 'number') ? `Релевантність: ${mainScore.toFixed(4)}` : 'Релевантність: N/A';

        header.appendChild(title);
        header.appendChild(scoreSpan);
        mainHtml.appendChild(header);

        const meta = document.createElement('div');
        meta.className = 'meta';
        const genresText = Array.isArray(main.genres) ? main.genres.join(', ') : '';
        meta.innerHTML = `<strong>Жанри:</strong> ${genresText || '—'}`;
        mainHtml.appendChild(meta);

        if (main.description) {
            const desc = document.createElement('div');
            desc.className = 'desc';
            desc.textContent = main.description;
            mainHtml.appendChild(desc);
        }

        resultsDiv.innerHTML = '';
        resultsDiv.appendChild(mainHtml);


        const recHeader = document.createElement('h3');
        recHeader.style.marginTop = '14px';
        recHeader.textContent = 'Схожі фільми:';
        resultsDiv.appendChild(recHeader);

        if (recs.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'empty';
            empty.textContent = 'Схожих фільмів не знайдено.';
            resultsDiv.appendChild(empty);
        } else {
            const recContainer = document.createElement('div');
            recContainer.className = 'rec-list';

            recs.forEach(r => {
                const rMovie = r.movie || {};
                const rScore = (typeof r.score === 'number') ? r.score : null;

                const itemDiv = document.createElement('div');
                itemDiv.className = 'rec-item';

                const rt = document.createElement('p');
                rt.className = 'rec-title';
                rt.textContent = rMovie.title || 'Назва відсутня';

                const rm = document.createElement('p');
                rm.className = 'rec-meta';
                const genres = Array.isArray(rMovie.genres) ? rMovie.genres.join(', ') : '';
                rm.innerHTML = `<strong>Жанри:</strong> ${genres || '—'} &nbsp; • &nbsp; <strong>Score:</strong> ${rScore !== null ? rScore.toFixed(4) : 'N/A'}`;

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
