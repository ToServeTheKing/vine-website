
const HistoryPage = () => {
  return (
    <div className="bg-bakery-50">
      {/* Page header */}
      <header className="container mx-auto px-4 pt-14 pb-10 md:pt-20 md:pb-12 text-center">
        <h1 className="font-adbhashitha text-4xl md:text-5xl text-bakery-900">
          Our story
        </h1>
      </header>

      {/* Story */}
      <div className="container mx-auto px-4 pb-16 md:pb-24">
        <div className="max-w-2xl mx-auto space-y-12">
          <section>
            <h2 className="font-adbhashitha text-2xl md:text-3xl text-bakery-900 mb-4">How it started</h2>
            <p className="text-bakery-800 leading-relaxed">
              Morissa Bennett opened The Vine in 2024, at 215 E Main Street, in the middle of downtown
              Princeville. The plan was not complicated. Bake it ourselves, sell it ourselves, and keep
              enough tables that nobody feels rushed out the door.
            </p>
          </section>

          <section>
            <h2 className="font-adbhashitha text-2xl md:text-3xl text-bakery-900 mb-4">What we make</h2>
            <p className="text-bakery-800 leading-relaxed">
              We opened with coffee and pastries. The menu kept growing. Now there are cinnamon rolls
              and caramel rolls, scones, cookie bars, macarons, brownies, and pies, plus sandwiches and
              paninis once the lunch crowd shows up.
            </p>
          </section>

          <section>
            <h2 className="font-adbhashitha text-2xl md:text-3xl text-bakery-900 mb-4">The cakes are the fun part</h2>
            <p className="text-bakery-800 leading-relaxed">
              Cakes and decorated cookies are made to order, which means we mostly bake whatever
              Princeville is celebrating that week. We have done a tractor, a cow, a 76th birthday, a
              retirement, a wedding, and a cake for the class of 1964. We have iced sugar cookies for
              the cross country team and for a bridal party. If you can describe it, we will have a go
              at it.
            </p>
          </section>

          <section>
            <h2 className="font-adbhashitha text-2xl md:text-3xl text-bakery-900 mb-4">Around town</h2>
            <p className="text-bakery-800 leading-relaxed">
              We turn out for Christmas in the Village every year and for other civic events, and the
              Princeville Civic Association counts us among the town&apos;s small businesses. Enjoy
              Illinois and Discover Peoria have both pointed travelers our way. If you are one of them,
              we open at 7:00am, Tuesday through Saturday.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
};

export default HistoryPage;
