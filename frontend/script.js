
const form = document.getElementById('search-form');
const input = document.getElementById('search-input');
const statusDiv = document.getElementById('status');
const resultsDiv = document.getElementById('results');
const suggestionsList = document.getElementById('search-suggestions');

const urlParams = new URLSearchParams(window.location.search);
const directorName = urlParams.get("name");

window.addEventListener('DOMContentLoaded', () => {
    if (directorName && document.getElementById('director-name')) {
        renderDirectorPage(directorName);
    }

    if (form) {
        setupMainPage();
    }
});

function setupMainPage() {
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
        setTimeout(() => { if(suggestionsList) suggestionsList.innerHTML = ''; }, 200);
    });
}

async function doSearch() {
    const q = input.value.trim();
    if (statusDiv) statusDiv.textContent = '';

    if (!q) {
        if (statusDiv) statusDiv.textContent = 'Будь ласка, введіть назву фільму.';
        return;
    }

    resultsDiv.innerHTML = '<div class="empty">Завантаження...</div>';

    try {
        const resp = await fetch(`/api/search?q=${encodeURIComponent(q)}`);
        const data = await resp.json();

        if (data.status === 'error' || !data.movie) {
            resultsDiv.innerHTML = `<div class="empty">${data.message || 'Фільм не знайдено.'}</div>`;
            return;
        }

        const mainCard = document.createElement('div');
        mainCard.className = 'result-card';

        const genresText = Array.isArray(data.movie.genres) ? data.movie.genres.join(', ') : '—';
        const directors = Array.isArray(data.movie.directors) ? data.movie.directors : [];
        const directorLinks = directors.map(name => {
            return `<a href="director.html?name=${encodeURIComponent(name)}" class="director-link">${name}</a>`;
        }).join(', ');

        mainCard.innerHTML = `
            <div class="movie-poster-placeholder">PICTURE</div>
            <div class="movie-info">
                <h2 class="title">${data.movie.title || 'Без назви'}</h2>
                <div class="meta">
                    <p><strong>Жанри:</strong> ${genresText}</p>
                    <p><strong>Режисер:</strong> ${directorLinks}</p>
                </div>
                <div class="desc">${data.movie.description || 'Опис відсутній.'}</div>
            </div>
        `;

        resultsDiv.innerHTML = '';
        resultsDiv.appendChild(mainCard);

        if (data.recommendations && data.recommendations.length > 0) {
            const recHeader = document.createElement('h3');
            recHeader.style.margin = '40px 0 20px 0';
            recHeader.textContent = 'Схожі фільми:';
            resultsDiv.appendChild(recHeader);

            data.recommendations.sort((a, b) => b.score - a.score);

            const recContainer = document.createElement('div');
            recContainer.className = 'rec-list';

            data.recommendations.forEach(r => {
                const itemDiv = document.createElement('div');
                itemDiv.className = 'rec-item';
                itemDiv.style.cursor = 'pointer';

                itemDiv.onclick = () => {
                    input.value = r.movie.title;
                    doSearch();
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                };

                const scoreVal = typeof r.score === 'number' ? r.score.toFixed(3) : '0';

                itemDiv.innerHTML = `
                    <p class="rec-title">${r.movie?.title || 'Назва'}</p>
                    <div class="rec-poster-mini">FILM</div>
                    <div style="margin-top:8px; font-size:11px; color:var(--accent); font-weight:bold;">
                        Score: ${scoreVal}
                    </div>
                `;
                recContainer.appendChild(itemDiv);
            });
            resultsDiv.appendChild(recContainer);
        }

    } catch (err) {
        console.error('Search error', err);
        resultsDiv.innerHTML = '<div class="empty">Помилка при отриманні результатів.</div>';
    }
}

async function renderDirectorPage(name) {
    const titleElem = document.getElementById('director-name');
    const gridElem = document.getElementById('movies-grid');

    if (titleElem) titleElem.textContent = name;
    document.title = `Режисер: ${name}`;

    try {
        const resp = await fetch(`/api/director?name=${encodeURIComponent(name)}`);
        const movies = await resp.json();

        if (!Array.isArray(movies) || movies.length === 0) {
            gridElem.innerHTML = '<div class="empty">Фільмів не знайдено.</div>';
            return;
        }

        gridElem.innerHTML = movies.map(m => `
            <div class="rec-item">
                <p class="rec-title">${m.title || 'Без назви'}</p>
                <div class="rec-poster-mini">FILM</div>
                <p style="font-size: 12px; color: #888; margin-top: 5px;">${m.year || ''}</p>
            </div>
        `).join('');
    } catch (err) {
        if (gridElem) gridElem.innerHTML = '<div class="empty">Помилка завантаження.</div>';
    }
}


