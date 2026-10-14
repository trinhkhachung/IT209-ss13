type Props = { query: string; category: string; onSubmit: (filters: { query: string; category: string }) => void };

export default function EventFilters({ query, category, onSubmit }: Props) {
  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    onSubmit({ query: String(data.get('query') || ''), category: String(data.get('category') || '') });
  }

  return <form className="filter-bar" onSubmit={handleSubmit}>
    <label className="filter-search"><span aria-hidden="true">⌕</span><span className="sr-only">Từ khóa</span>
      <input aria-label="Từ khóa" name="query" placeholder="Bạn muốn tìm điều gì?" defaultValue={query} /></label>
    <label className="filter-category"><span className="sr-only">Danh mục</span><select aria-label="Danh mục" name="category" defaultValue={category}>
      <option value="">Tất cả danh mục</option><option value="Công nghệ">Công nghệ</option><option value="Cộng đồng">Cộng đồng</option><option value="Nghệ thuật">Nghệ thuật</option><option value="Thể thao">Thể thao</option>
    </select></label>
    <button className="button button-dark" type="submit">Tìm sự kiện <span aria-hidden="true">↗</span></button>
  </form>;
}
